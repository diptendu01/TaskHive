import React, { useState } from 'react';
import { useDroppable } from '@dnd-kit/core';
import { KanbanCard } from './KanbanCard';
import { Button } from '../ui/Button';
import { Input, Textarea } from '../ui/Input';
import { PlusIcon, TrashIcon, XIcon } from '../ui/Icons';

export const KanbanColumn = ({
  list,
  cards,
  canMoveLeft = false,
  canMoveRight = false,
  onAddCard,
  onSelectCard,
  onDeleteCard,
  onRenameList,
  onDeleteList,
  onMoveList,
}) => {
  const { setNodeRef, isOver } = useDroppable({
    id: list.id,
    data: { type: 'list', list },
  });
  const [isAdding, setIsAdding] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [isRenaming, setIsRenaming] = useState(false);
  const [renameValue, setRenameValue] = useState(list.title);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmedTitle = title.trim();
    if (!trimmedTitle || submitting) return;

    setSubmitting(true);
    try {
      const ok = await onAddCard(list.id, trimmedTitle, description.trim() || undefined);
      if (ok !== false) {
        setTitle('');
        setDescription('');
        setIsAdding(false);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleRenameSubmit = async (e) => {
    e.preventDefault();
    const trimmed = renameValue.trim();
    if (!trimmed || trimmed === list.title) {
      setIsRenaming(false);
      setRenameValue(list.title);
      return;
    }
    await onRenameList?.(list.id, trimmed);
    setIsRenaming(false);
  };

  return (
    <section
      ref={setNodeRef}
      aria-label={`${list.title} column`}
      className={[
        'w-[312px] shrink-0 rounded-xl border bg-surface flex flex-col max-h-[calc(100vh-10.5rem)] transition-colors duration-150',
        isOver ? 'border-accent ring-1 ring-accent/50 bg-zinc-950' : 'border-border',
      ].join(' ')}
    >
      <div className="px-3.5 py-3 border-b border-border/80 flex items-center justify-between gap-2">
        {isRenaming ? (
          <form onSubmit={handleRenameSubmit} className="flex-1 flex items-center gap-1.5">
            <Input
              type="text"
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              onBlur={handleRenameSubmit}
              autoFocus
              maxLength={120}
              aria-label="Rename list"
              className="h-7 text-xs bg-black"
            />
          </form>
        ) : (
          <button
            type="button"
            onClick={() => {
              setRenameValue(list.title);
              setIsRenaming(true);
            }}
            title="Click to rename list"
            className="text-xs font-semibold uppercase tracking-wider text-foreground truncate text-left hover:text-accent transition-colors cursor-pointer"
          >
            {list.title}
          </button>
        )}

        <div className="flex items-center gap-1 shrink-0">
          {canMoveLeft && onMoveList && (
            <button
              type="button"
              onClick={() => onMoveList(list.id, -1)}
              aria-label={`Move ${list.title} left`}
              title="Move list left"
              className="rounded p-1 text-muted-foreground hover:text-foreground hover:bg-zinc-800 transition-colors cursor-pointer"
            >
              <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="m15 18-6-6 6-6" />
              </svg>
            </button>
          )}
          {canMoveRight && onMoveList && (
            <button
              type="button"
              onClick={() => onMoveList(list.id, 1)}
              aria-label={`Move ${list.title} right`}
              title="Move list right"
              className="rounded p-1 text-muted-foreground hover:text-foreground hover:bg-zinc-800 transition-colors cursor-pointer"
            >
              <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="m9 18 6-6-6-6" />
              </svg>
            </button>
          )}
          <span className="inline-flex h-5 min-w-[20px] items-center justify-center rounded-full border border-border bg-card px-1.5 font-mono text-[11px] text-muted-foreground">
            {cards.length}
          </span>
          {onDeleteList && (
            <button
              type="button"
              onClick={() => onDeleteList(list.id)}
              aria-label={`Delete ${list.title} list`}
              title="Delete list"
              className="rounded p-1 text-muted-foreground hover:text-red-400 hover:bg-red-950/60 transition-colors cursor-pointer"
            >
              <TrashIcon className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-2.5 space-y-2.5 min-h-[120px]">
        {cards.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border/80 px-3 py-6 text-center text-xs text-muted-foreground">
            Drop a card here or add a new task below.
          </div>
        ) : (
          cards.map((card) => (
            <KanbanCard
              key={card.id}
              card={card}
              onSelect={onSelectCard}
              onQuickMove={onSelectCard}
              onDelete={onDeleteCard}
            />
          ))
        )}
      </div>

      <div className="p-2.5 border-t border-border/80">
        {isAdding ? (
          <form onSubmit={handleSubmit} className="space-y-2">
            <Input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Card title..."
              aria-label={`New card title in ${list.title}`}
              autoFocus
              required
              maxLength={512}
              className="h-8 text-xs bg-black"
            />
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description..."
              aria-label="Optional card description"
              rows={2}
              maxLength={4096}
              className="min-h-[56px] text-xs bg-black"
            />
            <div className="flex items-center gap-1.5">
              <Button type="submit" size="sm" disabled={submitting} className="flex-1">
                {submitting ? 'Adding...' : 'Add Card'}
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Cancel adding card"
                onClick={() => {
                  setIsAdding(false);
                  setTitle('');
                  setDescription('');
                }}
              >
                <XIcon className="h-3.5 w-3.5" />
              </Button>
            </div>
          </form>
        ) : (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsAdding(true)}
            className="w-full justify-start text-muted-foreground hover:text-foreground"
          >
            <PlusIcon className="h-3.5 w-3.5" />
            <span>Add card</span>
          </Button>
        )}
      </div>
    </section>
  );
};
