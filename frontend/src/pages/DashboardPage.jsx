import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { api } from '../services/api';
import { AppHeader } from '../components/layout/AppHeader';
import { BoardCard } from '../components/dashboard/BoardCard';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { Badge } from '../components/ui/Badge';
import { LayersIcon, PlusIcon, SearchIcon } from '../components/ui/Icons';

const BOARD_COLORS = [
  { label: 'Teal', value: '#14b8a6' },
  { label: 'Blue', value: '#3b82f6' },
  { label: 'Violet', value: '#a855f7' },
  { label: 'Amber', value: '#f59e0b' },
  { label: 'Rose', value: '#f43f5e' },
  { label: 'White', value: '#fafafa' },
];

const DashboardPage = () => {
  const { token, logout } = useAuth();
  const [boards, setBoards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [newBoardName, setNewBoardName] = useState('');
  const [selectedColor, setSelectedColor] = useState(BOARD_COLORS[0].value);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    if (!token) return undefined;

    const controller = new AbortController();
    setLoading(true);
    setError('');

    api
      .getBoards(token, { onUnauthorized: logout, signal: controller.signal })
      .then((data) => {
        if (Array.isArray(data)) {
          setBoards(data);
        }
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          setError(err.message || 'Failed to load boards');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [token, logout]);

  const filteredBoards = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return boards;
    return boards.filter(
      (board) =>
        board.name?.toLowerCase().includes(q) || board.slug?.toLowerCase().includes(q)
    );
  }, [boards, searchQuery]);

  const handleCreateBoard = async (e) => {
    e.preventDefault();
    const trimmed = newBoardName.trim();
    if (!trimmed || creating) return;

    const tempId = `temp-${Date.now()}`;
    const optimisticBoard = {
      id: tempId,
      name: trimmed,
      slug: 'creating...',
      avatarColor: selectedColor,
      pending: true,
    };

    setCreating(true);
    setError('');
    setNewBoardName('');
    setBoards((prev) => [...prev, optimisticBoard]);

    try {
      const created = await api.createBoard(
        token,
        { name: trimmed, avatarColor: selectedColor },
        { onUnauthorized: logout }
      );
      setBoards((prev) => prev.map((b) => (b.id === tempId ? created : b)));
    } catch (err) {
      setBoards((prev) => prev.filter((b) => b.id !== tempId));
      setNewBoardName(trimmed);
      setError(err.message || 'Failed to create board');
    } finally {
      setCreating(false);
    }
  };

  const handleRenameBoard = async (boardId, name, avatarColor) => {
    const previous = boards;
    setError('');
    setBoards((prev) => prev.map((b) => (b.id === boardId ? { ...b, name } : b)));
    try {
      const updated = await api.updateBoard(
        token,
        boardId,
        { name, avatarColor },
        { onUnauthorized: logout }
      );
      setBoards((prev) => prev.map((b) => (b.id === boardId ? updated : b)));
    } catch (err) {
      setBoards(previous);
      setError(err.message || 'Failed to rename board');
    }
  };

  const handleDeleteBoard = async (boardId) => {
    const previous = boards;
    setError('');
    setBoards((prev) => prev.filter((b) => b.id !== boardId));
    try {
      await api.deleteBoard(token, boardId, { onUnauthorized: logout });
    } catch (err) {
      setBoards(previous);
      setError(err.message || 'Failed to delete board');
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      <AppHeader />

      <main id="main-content" className="flex-1 w-full max-w-6xl mx-auto px-4 sm:px-6 py-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-6 border-b border-border">
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-2xl font-semibold tracking-tight text-foreground">
                Your Boards
              </h1>
              <Badge variant="outline" className="font-mono text-[11px]">
                {boards.length}
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground mt-1">
              Select a board to collaborate in real time or create a new workspace.
            </p>
          </div>

          <div className="relative w-full md:w-64">
            <SearchIcon className="h-4 w-4 text-muted-foreground absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <Input
              type="search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Filter boards..."
              aria-label="Filter boards"
              className="pl-9 bg-surface"
            />
          </div>
        </div>

        <section
          aria-label="Create a new board"
          className="mt-6 rounded-xl border border-border bg-surface p-4"
        >
          <form
            onSubmit={handleCreateBoard}
            className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3"
          >
            <Input
              type="text"
              value={newBoardName}
              onChange={(e) => setNewBoardName(e.target.value)}
              placeholder="New board name (e.g. Q4 Product Roadmap)..."
              aria-label="New board name"
              required
              maxLength={120}
              className="flex-1 bg-black"
            />

            <div className="flex items-center justify-between sm:justify-start gap-3">
              <div
                className="flex items-center gap-1.5"
                role="radiogroup"
                aria-label="Board accent color"
              >
                {BOARD_COLORS.map((color) => {
                  const active = selectedColor === color.value;
                  return (
                    <button
                      key={color.value}
                      type="button"
                      role="radio"
                      aria-checked={active}
                      aria-label={color.label}
                      onClick={() => setSelectedColor(color.value)}
                      className={[
                        'h-5 w-5 rounded-full cursor-pointer transition-transform',
                        active
                          ? 'ring-2 ring-foreground ring-offset-2 ring-offset-surface'
                          : 'opacity-70 hover:opacity-100',
                      ].join(' ')}
                      style={{ backgroundColor: color.value }}
                    />
                  );
                })}
              </div>

              <Button type="submit" disabled={creating} size="md">
                <PlusIcon className="h-4 w-4" />
                <span>{creating ? 'Creating...' : 'Create Board'}</span>
              </Button>
            </div>
          </form>
        </section>

        {error && (
          <Alert onDismiss={() => setError('')} className="mt-5">
            {error}
          </Alert>
        )}

        <section aria-label="Board list" className="mt-6">
          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {[1, 2, 3].map((item) => (
                <div
                  key={item}
                  className="h-32 rounded-xl border border-border bg-surface animate-pulse p-5"
                >
                  <div className="h-4 w-1/2 rounded bg-secondary" />
                  <div className="h-3 w-1/3 rounded bg-secondary/70 mt-8" />
                </div>
              ))}
            </div>
          ) : filteredBoards.length === 0 ? (
            <div className="rounded-xl border border-dashed border-border bg-surface/60 px-6 py-14 text-center">
              <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-lg border border-border bg-card text-muted-foreground mb-3">
                <LayersIcon className="h-5 w-5" />
              </div>
              <p className="text-sm font-medium text-foreground">
                {boards.length === 0 ? 'No boards yet' : 'No matching boards'}
              </p>
              <p className="text-xs text-muted-foreground mt-1">
                {boards.length === 0
                  ? 'Create your first Kanban board above to get started.'
                  : 'Try adjusting your search query.'}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredBoards.map((board) => (
                <BoardCard
                  key={board.id}
                  board={board}
                  onRename={handleRenameBoard}
                  onDelete={handleDeleteBoard}
                />
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export default DashboardPage;
