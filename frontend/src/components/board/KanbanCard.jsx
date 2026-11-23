import React, { useCallback } from 'react';
import { useDraggable, useDroppable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import { AlignLeftIcon, GripVerticalIcon, MoveHorizontalIcon, TrashIcon } from '../ui/Icons';

export const KanbanCardView = ({
  card,
  isOverlay = false,
  isDragging = false,
  isDropTarget = false,
  dragHandleProps = {},
  onSelect,
  onQuickMove,
  onDelete,
}) => {
  const handleMouseMove = (e) => {
    if (isOverlay) return;
    const rect = e.currentTarget.getBoundingClientRect();
    e.currentTarget.style.setProperty('--mx', `${e.clientX - rect.left}px`);
    e.currentTarget.style.setProperty('--my', `${e.clientY - rect.top}px`);
  };

  return (
    <div
      onMouseMove={handleMouseMove}
      className={[
        'group rounded-lg border bg-card p-3 transition-colors duration-150',
        isOverlay
          ? 'border-accent/70 bg-secondary shadow-2xl ring-1 ring-accent/40 cursor-grabbing'
          : 'spotlight-card border-border',
        isDropTarget && !isDragging && !isOverlay
          ? 'border-t-2 border-t-accent ring-1 ring-accent/40'
          : '',
        isDragging && !isOverlay ? 'opacity-35' : 'opacity-100',
      ].join(' ')}
    >
      <div className="relative z-10 flex items-start gap-2">
        <button
          type="button"
          aria-label={`Drag ${card.title}`}
          className="mt-0.5 -ml-1 p-1 rounded text-muted-foreground/60 hover:text-foreground hover:bg-secondary cursor-grab active:cursor-grabbing shrink-0"
          {...dragHandleProps}
        >
          <GripVerticalIcon className="h-3.5 w-3.5" />
        </button>

        <div className="min-w-0 flex-1">
          <button
            type="button"
            onClick={() => onSelect?.(card)}
            className="w-full text-left cursor-pointer focus-visible:outline-none focus-visible:underline"
          >
            <p className="text-sm font-medium text-foreground leading-snug break-words">
              {card.title}
            </p>
            {card.description && (
              <p className="mt-1.5 text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                {card.description}
              </p>
            )}
          </button>

          <div className="mt-2.5 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
              {card.description && (
                <span className="inline-flex items-center gap-1" title="Has description">
                  <AlignLeftIcon className="h-3 w-3" />
                  <span>Details</span>
                </span>
              )}
            </div>

            {!isOverlay && (
              <div className="flex items-center gap-1 opacity-70 group-hover:opacity-100 group-focus-within:opacity-100 transition-opacity duration-150">
                {onQuickMove && (
                  <button
                    type="button"
                    onClick={() => onQuickMove(card)}
                    aria-label={`Edit or move ${card.title}`}
                    title="Edit / Move card"
                    className="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-[11px] text-muted-foreground hover:text-foreground hover:bg-zinc-800 cursor-pointer transition-colors"
                  >
                    <MoveHorizontalIcon className="h-3 w-3" />
                    <span>Edit</span>
                  </button>
                )}
                {onDelete && (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onDelete(card.id);
                    }}
                    aria-label={`Delete ${card.title}`}
                    title="Delete card"
                    className="inline-flex items-center justify-center rounded p-1 text-muted-foreground hover:text-red-400 hover:bg-red-950/60 cursor-pointer transition-colors"
                  >
                    <TrashIcon className="h-3 w-3" />
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export const KanbanCard = ({ card, onSelect, onQuickMove, onDelete }) => {
  const { attributes, listeners, setNodeRef: setDragRef, transform, isDragging } = useDraggable({
    id: card.id,
    data: { type: 'card', card },
  });

  const { setNodeRef: setDropRef, isOver } = useDroppable({
    id: card.id,
    data: { type: 'card', card },
  });

  const setCombinedRef = useCallback(
    (node) => {
      setDragRef(node);
      setDropRef(node);
    },
    [setDragRef, setDropRef]
  );

  const style = transform
    ? { transform: CSS.Translate.toString(transform) }
    : undefined;

  return (
    <div ref={setCombinedRef} style={style}>
      <KanbanCardView
        card={card}
        isDragging={isDragging}
        isDropTarget={isOver}
        dragHandleProps={{ ...listeners, ...attributes }}
        onSelect={onSelect}
        onQuickMove={onQuickMove}
        onDelete={onDelete}
      />
    </div>
  );
};
