import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { useAuth } from '../hooks/useAuth';
import { api } from '../services/api';
import { createStompClient } from '../services/stompClient';
import { AppHeader } from '../components/layout/AppHeader';
import { KanbanColumn } from '../components/board/KanbanColumn';
import { KanbanCardView } from '../components/board/KanbanCard';
import { CardDetailModal } from '../components/board/CardDetailModal';
import { DELETE_DROP_ZONE_ID, DeleteDropZone } from '../components/board/DeleteDropZone';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { ArrowLeftIcon, PlusIcon, SearchIcon, XIcon } from '../components/ui/Icons';

function resolveWsUrl() {
  const configured = (import.meta.env.VITE_WS_URL || '').trim();
  if (configured) return configured;

  const apiBase = (import.meta.env.VITE_API_URL || '').trim().replace(/\/+$/, '');
  if (apiBase) {
    return `${apiBase.replace(/^http/, 'ws')}/ws`;
  }

  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${wsProtocol}//${window.location.host}/ws`;
}

const BoardPage = () => {
  const { token, logout } = useAuth();
  const { boardId } = useParams();

  const [boardName, setBoardName] = useState('');
  const [boardColor, setBoardColor] = useState('#14b8a6');
  const [lists, setLists] = useState([]);
  const [cards, setCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [wsStatus, setWsStatus] = useState('connecting'); // connected | reconnecting | offline
  const [searchQuery, setSearchQuery] = useState('');
  const [addingList, setAddingList] = useState(false);
  const [newListTitle, setNewListTitle] = useState('');
  const [creatingList, setCreatingList] = useState(false);
  const [activeCardId, setActiveCardId] = useState(null);
  const [selectedCardId, setSelectedCardId] = useState(null);

  const stompClientRef = useRef(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor)
  );

  useEffect(() => {
    if (!token || !boardId) return undefined;

    let cancelled = false;
    const controller = new AbortController();
    setLoading(true);
    setError('');

    api
      .getBoard(token, boardId, { onUnauthorized: logout, signal: controller.signal })
      .then((data) => {
        if (!cancelled && data) {
          setBoardName(data.name || '');
          setBoardColor(data.avatarColor || '#14b8a6');
          setLists(data.lists || []);
          setCards(data.cards || []);
        }
      })
      .catch((err) => {
        if (!cancelled && err.name !== 'AbortError') {
          setError(err.message || 'Failed to load board');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    const client = createStompClient(resolveWsUrl(), {
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        if (cancelled) return;
        setWsStatus('connected');
        client.subscribe(`/topic/board/${boardId}`, (msg) => {
          const { type, data } = msg || {};
          if (type === 'card:moved' && data?.newCard) {
            setCards((prev) =>
              prev.map((c) => (c.id === data.cardId ? data.newCard : c))
            );
          } else if (type === 'card:created' && data?.card) {
            setCards((prev) =>
              prev.some((c) => c.id === data.card.id) ? prev : [...prev, data.card]
            );
          } else if (type === 'card:deleted' && data?.cardId) {
            setCards((prev) => prev.filter((c) => c.id !== data.cardId));
            setSelectedCardId((prev) => (prev === data.cardId ? null : prev));
          } else if (type === 'list:created' && data?.list) {
            setLists((prev) =>
              prev.some((l) => l.id === data.list.id) ? prev : [...prev, data.list]
            );
          } else if (type === 'list:reordered' && Array.isArray(data?.newLists)) {
            setLists(data.newLists);
          }
        });
      },
      onReconnecting: () => {
        if (!cancelled) setWsStatus('reconnecting');
      },
      onDisconnect: () => {
        if (!cancelled) setWsStatus('offline');
      },
      onError: () => {
        if (!cancelled) setWsStatus('offline');
      },
    });

    stompClientRef.current = client;
    client.connect();

    return () => {
      cancelled = true;
      controller.abort();
      client.disconnect();
      stompClientRef.current = null;
    };
  }, [token, boardId, logout]);

  const sortedLists = useMemo(
    () => lists.slice().sort((a, b) => a.position - b.position),
    [lists]
  );

  const filteredCards = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return cards;
    return cards.filter(
      (c) =>
        c.title?.toLowerCase().includes(q) || c.description?.toLowerCase().includes(q)
    );
  }, [cards, searchQuery]);

  const activeCard = useMemo(
    () => (activeCardId ? cards.find((c) => c.id === activeCardId) : null),
    [cards, activeCardId]
  );

  const selectedCard = useMemo(
    () => (selectedCardId ? cards.find((c) => c.id === selectedCardId) : null),
    [cards, selectedCardId]
  );

  const handleAddList = async (e) => {
    e.preventDefault();
    const trimmed = newListTitle.trim();
    if (!trimmed || creatingList) return;

    setCreatingList(true);
    setError('');
    try {
      const data = await api.createList(
        token,
        boardId,
        { title: trimmed },
        { onUnauthorized: logout }
      );
      setLists((prev) => (prev.some((l) => l.id === data.id) ? prev : [...prev, data]));
      setNewListTitle('');
      setAddingList(false);
    } catch (err) {
      setError(err.message || 'Failed to create list');
    } finally {
      setCreatingList(false);
    }
  };

  const handleAddCard = useCallback(
    async (listId, title, description) => {
      setError('');
      try {
        const data = await api.createCard(
          token,
          boardId,
          { listId, title, description },
          { onUnauthorized: logout }
        );
        setCards((prev) => (prev.some((c) => c.id === data.id) ? prev : [...prev, data]));
        return true;
      } catch (err) {
        setError(err.message || 'Failed to create card');
        return false;
      }
    },
    [token, boardId, logout]
  );

  const handleDeleteCard = useCallback(
    async (cardId) => {
      const previousCards = cards;
      setError('');
      setCards((prev) => prev.filter((c) => c.id !== cardId));
      setSelectedCardId((prev) => (prev === cardId ? null : prev));

      try {
        await api.deleteCard(token, boardId, cardId, { onUnauthorized: logout });
      } catch (err) {
        setCards(previousCards);
        setError(err.message || 'Failed to delete card');
      }
    },
    [cards, token, boardId, logout]
  );

  const moveCardToList = useCallback(
    async (cardId, newListId) => {
      const draggedCard = cards.find((c) => c.id === cardId);
      if (!draggedCard || draggedCard.listId === newListId) return;

      const previousCards = cards;
      const targetCards = cards
        .filter((c) => c.listId === newListId)
        .sort((a, b) => a.position - b.position);
      const newPosition =
        targetCards.length > 0
          ? targetCards[targetCards.length - 1].position + 1000.0
          : 1000.0;

      setCards((prev) =>
        prev.map((c) =>
          c.id === cardId ? { ...c, listId: newListId, position: newPosition } : c
        )
      );

      const payload = { cardId, newListId, newPosition };
      const sentViaWs = stompClientRef.current?.publish('/app/card/move', payload);

      if (!sentViaWs) {
        try {
          await api.moveCard(token, boardId, cardId, payload, { onUnauthorized: logout });
        } catch (err) {
          setCards(previousCards);
          setError(err.message || 'Failed to move card');
        }
      }
    },
    [cards, token, boardId, logout]
  );

  const handleDragStart = (event) => {
    setActiveCardId(event.active.id);
  };

  const handleDragEnd = (event) => {
    setActiveCardId(null);
    const { active, over } = event;
    if (!over) return;

    if (over.id === DELETE_DROP_ZONE_ID) {
      handleDeleteCard(active.id);
      return;
    }

    moveCardToList(active.id, over.id);
  };

  const statusBadge =
    wsStatus === 'connected' ? (
      <Badge variant="success">
        <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" aria-hidden="true" />
        Live
      </Badge>
    ) : wsStatus === 'reconnecting' ? (
      <Badge variant="warning">
        <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" aria-hidden="true" />
        Reconnecting
      </Badge>
    ) : (
      <Badge variant="outline">
        <span className="h-1.5 w-1.5 rounded-full bg-zinc-500" aria-hidden="true" />
        Offline
      </Badge>
    );

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      <AppHeader
        leftSlot={
          <div className="flex items-center gap-2.5 min-w-0">
            <Link
              to="/"
              className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors shrink-0"
            >
              <ArrowLeftIcon className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Boards</span>
            </Link>
            <span className="text-border" aria-hidden="true">
              /
            </span>
            <span
              className="h-2 w-2 rounded-full shrink-0"
              style={{ backgroundColor: boardColor }}
              aria-hidden="true"
            />
            <h1 className="text-sm font-semibold text-foreground truncate">
              {boardName || `Board ${boardId}`}
            </h1>
            {statusBadge}
          </div>
        }
        rightSlot={
          <div className="relative hidden md:block w-52">
            <SearchIcon className="h-3.5 w-3.5 text-muted-foreground absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <Input
              type="search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Filter cards..."
              aria-label="Filter cards on this board"
              className="h-8 pl-8 text-xs bg-surface"
            />
          </div>
        }
      />

      <main id="main-content" className="flex-1 flex flex-col px-4 sm:px-6 pt-5 pb-6 overflow-hidden">
        {error && (
          <Alert onDismiss={() => setError('')} className="mb-4 max-w-2xl">
            {error}
          </Alert>
        )}

        {loading ? (
          <div className="flex gap-4 overflow-x-auto pb-4">
            {[1, 2, 3].map((col) => (
              <div
                key={col}
                className="w-[312px] shrink-0 h-80 rounded-xl border border-border bg-surface animate-pulse p-4 space-y-3"
              >
                <div className="h-4 w-24 rounded bg-secondary" />
                <div className="h-20 rounded-lg bg-card" />
                <div className="h-16 rounded-lg bg-card" />
              </div>
            ))}
          </div>
        ) : (
          <DndContext
            sensors={sensors}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
            onDragCancel={() => setActiveCardId(null)}
          >
            <div className="flex-1 flex items-start gap-4 overflow-x-auto pb-4">
              {sortedLists.map((list) => {
                const listCards = filteredCards
                  .filter((c) => c.listId === list.id)
                  .sort((a, b) => a.position - b.position);
                return (
                  <KanbanColumn
                    key={list.id}
                    list={list}
                    cards={listCards}
                    onAddCard={handleAddCard}
                    onSelectCard={(card) => setSelectedCardId(card.id)}
                    onDeleteCard={handleDeleteCard}
                  />
                );
              })}

              <div className="w-[312px] shrink-0">
                {addingList ? (
                  <form
                    onSubmit={handleAddList}
                    className="rounded-xl border border-border bg-surface p-3 space-y-2.5"
                  >
                    <Input
                      type="text"
                      value={newListTitle}
                      onChange={(e) => setNewListTitle(e.target.value)}
                      placeholder="Enter list title..."
                      aria-label="New list title"
                      autoFocus
                      required
                      maxLength={120}
                      className="h-8 text-xs bg-black"
                    />
                    <div className="flex items-center gap-1.5">
                      <Button type="submit" size="sm" disabled={creatingList} className="flex-1">
                        {creatingList ? 'Adding...' : 'Add List'}
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label="Cancel adding list"
                        onClick={() => {
                          setAddingList(false);
                          setNewListTitle('');
                        }}
                      >
                        <XIcon className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </form>
                ) : (
                  <button
                    type="button"
                    onClick={() => setAddingList(true)}
                    className="w-full h-11 rounded-xl border border-dashed border-border bg-surface/50 hover:bg-surface hover:border-zinc-600 text-xs font-medium text-muted-foreground hover:text-foreground flex items-center justify-center gap-2 cursor-pointer transition-colors"
                  >
                    <PlusIcon className="h-4 w-4" />
                    <span>Add another list</span>
                  </button>
                )}
              </div>
            </div>

            <DeleteDropZone visible={Boolean(activeCardId)} />

            <DragOverlay>
              {activeCard ? <KanbanCardView card={activeCard} isOverlay /> : null}
            </DragOverlay>
          </DndContext>
        )}
      </main>

      <CardDetailModal
        card={selectedCard}
        lists={sortedLists}
        onClose={() => setSelectedCardId(null)}
        onMoveToList={moveCardToList}
        onDeleteCard={handleDeleteCard}
      />
    </div>
  );
};

export default BoardPage;
