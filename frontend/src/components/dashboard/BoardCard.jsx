import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowUpRightIcon } from '../ui/Icons';

export const BoardCard = ({ board }) => {
  const accentColor = board.avatarColor || '#14b8a6';

  const handleMouseMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    e.currentTarget.style.setProperty('--mx', `${e.clientX - rect.left}px`);
    e.currentTarget.style.setProperty('--my', `${e.clientY - rect.top}px`);
  };

  return (
    <Link
      to={`/boards/${board.id}`}
      onMouseMove={handleMouseMove}
      className="spotlight-card group relative flex flex-col justify-between rounded-xl border border-border bg-card p-5 transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div className="relative z-10 flex items-start justify-between gap-3">
        <div className="flex items-center gap-2.5 min-w-0">
          <span
            className="h-2.5 w-2.5 rounded-full shrink-0 ring-4 ring-white/5"
            style={{ backgroundColor: accentColor }}
            aria-hidden="true"
          />
          <h3 className="text-sm font-semibold text-foreground truncate group-hover:text-white">
            {board.name}
          </h3>
        </div>
        <ArrowUpRightIcon className="h-4 w-4 text-muted-foreground shrink-0 transition-colors group-hover:text-foreground" />
      </div>

      <div className="relative z-10 mt-5 pt-3 border-t border-border/70 flex items-center justify-between gap-2">
        <span className="font-mono text-[11px] text-muted-foreground truncate">
          /{board.slug}
        </span>
        <span className="text-[11px] font-medium text-muted-foreground group-hover:text-accent transition-colors">
          Open board
        </span>
      </div>
    </Link>
  );
};
