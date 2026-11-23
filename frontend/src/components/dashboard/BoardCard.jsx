import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowUpRightIcon, TrashIcon } from '../ui/Icons';
import { Input } from '../ui/Input';

export const BoardCard = ({ board, onRename, onDelete }) => {
  const accentColor = board.avatarColor || '#14b8a6';
  const [isRenaming, setIsRenaming] = useState(false);
  const [nameInput, setNameInput] = useState(board.name);

  const handleMouseMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    e.currentTarget.style.setProperty('--mx', `${e.clientX - rect.left}px`);
    e.currentTarget.style.setProperty('--my', `${e.clientY - rect.top}px`);
  };

  const handleRenameSubmit = async (e) => {
    e.preventDefault();
    const trimmed = nameInput.trim();
    if (!trimmed || trimmed === board.name) {
      setIsRenaming(false);
      setNameInput(board.name);
      return;
    }
    await onRename?.(board.id, trimmed, board.avatarColor);
    setIsRenaming(false);
  };

  return (
    <div
      onMouseMove={handleMouseMove}
      className={[
        'spotlight-card group relative flex flex-col justify-between rounded-xl border border-border bg-card p-5 transition-colors duration-150',
        board.pending ? 'opacity-70 pointer-events-none' : '',
      ].join(' ')}
    >
      <div className="relative z-10 flex items-start justify-between gap-3">
        <div className="flex items-center gap-2.5 min-w-0 flex-1">
          <span
            className="h-2.5 w-2.5 rounded-full shrink-0 ring-4 ring-white/5"
            style={{ backgroundColor: accentColor }}
            aria-hidden="true"
          />
          {isRenaming ? (
            <form onSubmit={handleRenameSubmit} className="flex-1">
              <Input
                type="text"
                value={nameInput}
                onChange={(e) => setNameInput(e.target.value)}
                onBlur={handleRenameSubmit}
                autoFocus
                maxLength={120}
                aria-label="Rename board"
                className="h-7 text-xs bg-black"
              />
            </form>
          ) : (
            <Link
              to={`/boards/${board.id}`}
              className="text-sm font-semibold text-foreground truncate hover:text-white focus-visible:outline-none focus-visible:underline"
            >
              {board.name}
            </Link>
          )}
        </div>

        <div className="flex items-center gap-1 shrink-0">
          {onRename && !isRenaming && (
            <button
              type="button"
              onClick={() => {
                setNameInput(board.name);
                setIsRenaming(true);
              }}
              aria-label={`Rename ${board.name}`}
              title="Rename board"
              className="rounded p-1 text-muted-foreground opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 hover:text-foreground hover:bg-zinc-800 transition-all cursor-pointer"
            >
              <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
              </svg>
            </button>
          )}
          {onDelete && (
            <button
              type="button"
              onClick={() => onDelete(board.id)}
              aria-label={`Delete ${board.name}`}
              title="Delete board"
              className="rounded p-1 text-muted-foreground opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 hover:text-red-400 hover:bg-red-950/60 transition-all cursor-pointer"
            >
              <TrashIcon className="h-3.5 w-3.5" />
            </button>
          )}
          <Link
            to={`/boards/${board.id}`}
            aria-label={`Open ${board.name}`}
            className="rounded p-1 text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowUpRightIcon className="h-4 w-4" />
          </Link>
        </div>
      </div>

      <Link
        to={`/boards/${board.id}`}
        className="relative z-10 mt-5 pt-3 border-t border-border/70 flex items-center justify-between gap-2"
      >
        <span className="font-mono text-[11px] text-muted-foreground truncate">
          /{board.slug}
        </span>
        <span className="text-[11px] font-medium text-muted-foreground group-hover:text-accent transition-colors">
          {board.pending ? 'Creating...' : 'Open board'}
        </span>
      </Link>
    </div>
  );
};
