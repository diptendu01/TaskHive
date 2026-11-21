import React, { useEffect } from 'react';
import { XIcon } from './Icons';

export const Modal = ({ open, onClose, title, description, children }) => {
  useEffect(() => {
    if (!open) return undefined;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg rounded-xl border border-border bg-surface text-foreground shadow-2xl overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
          <div className="min-w-0">
            <h2 id="modal-title" className="text-base font-semibold text-foreground leading-snug">
              {title}
            </h2>
            {description && (
              <p className="text-xs text-muted-foreground mt-1">{description}</p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="rounded-md p-1.5 text-muted-foreground hover:text-foreground hover:bg-secondary cursor-pointer transition-colors"
          >
            <XIcon className="h-4 w-4" />
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
};
