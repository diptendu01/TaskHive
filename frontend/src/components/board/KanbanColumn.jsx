import React, { useState } from 'react';
import { useDroppable } from '@dnd-kit/core';
import { KanbanCard } from './KanbanCard';
import { Button } from '../ui/Button';
import { Input, Textarea } from '../ui/Input';
import { PlusIcon, XIcon } from '../ui/Icons';

export const KanbanColumn = ({ list, cards, onAddCard, onSelectCard, onDeleteCard }) => {
  const { setNodeRef, isOver } = useDroppable({ id: list.id });
  const [isAdding, setIsAdding] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);

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
        <h2 className="text-xs font-semibold uppercase tracking-wider text-foreground truncate">
          {list.title}
        </h2>
        <span className="inline-flex h-5 min-w-[20px] items-center justify-center rounded-full border border-border bg-card px-1.5 font-mono text-[11px] text-muted-foreground">
          {cards.length}
        </span>
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
