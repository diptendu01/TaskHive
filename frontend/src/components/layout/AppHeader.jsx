import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../ui/Button';
import { KanbanIcon, LogOutIcon } from '../ui/Icons';

function getInitials(name, email) {
  const source = (name || email || 'U').trim();
  const parts = source.split(/\s+/);
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  }
  return source.slice(0, 2).toUpperCase();
}

export const AppHeader = ({ leftSlot = null, rightSlot = null }) => {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 h-14 border-b border-border bg-black/85 backdrop-blur-md">
      <div className="h-full max-w-[1600px] mx-auto px-4 sm:px-6 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3 min-w-0">
          <Link
            to="/"
            className="flex items-center gap-2.5 text-foreground hover:opacity-90 transition-opacity shrink-0"
          >
            <span className="flex h-7 w-7 items-center justify-center rounded-md border border-border bg-card text-accent">
              <KanbanIcon className="h-4 w-4" />
            </span>
            <span className="text-sm font-semibold tracking-tight">TaskHive</span>
          </Link>

          {leftSlot && (
            <>
              <span className="h-4 w-px bg-border shrink-0" aria-hidden="true" />
              <div className="flex items-center gap-2.5 min-w-0">{leftSlot}</div>
            </>
          )}
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          {rightSlot}
          {user && (
            <div className="hidden sm:flex items-center gap-2 pl-2 border-l border-border">
              <span
                className="flex h-7 w-7 items-center justify-center rounded-full border border-border bg-secondary text-[11px] font-mono font-medium text-foreground"
                aria-hidden="true"
              >
                {getInitials(user.name, user.email)}
              </span>
              <span className="text-xs text-muted-foreground max-w-[150px] truncate">
                {user.name || user.email}
              </span>
            </div>
          )}
          <Button
            variant="ghost"
            size="sm"
            onClick={logout}
            aria-label="Sign out"
            className="text-muted-foreground hover:text-foreground"
          >
            <LogOutIcon className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">Sign Out</span>
          </Button>
        </div>
      </div>
    </header>
  );
};
