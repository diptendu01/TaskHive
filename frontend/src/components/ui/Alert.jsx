import React from 'react';
import { AlertCircleIcon, XIcon } from './Icons';

export const Alert = ({ children, onDismiss, className = '' }) => {
  if (!children) return null;

  return (
    <div
      role="alert"
      aria-live="assertive"
      className={[
        'flex items-start gap-2.5 rounded-lg border border-red-500/30 bg-red-950/40 px-3.5 py-2.5 text-sm text-red-200',
        className,
      ].join(' ')}
    >
      <AlertCircleIcon className="h-4 w-4 text-red-400 shrink-0 mt-0.5" />
      <div className="flex-1 leading-snug">{children}</div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Dismiss error"
          className="text-red-300/80 hover:text-red-100 cursor-pointer p-0.5 rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <XIcon className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
};
