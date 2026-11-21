import React from 'react';

const sharedInputStyles = [
  'w-full rounded-md border border-input bg-surface px-3 py-2 text-sm text-foreground',
  'placeholder:text-muted-foreground/70 transition-colors duration-150',
  'hover:border-zinc-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-ring',
  'disabled:cursor-not-allowed disabled:opacity-50',
].join(' ');

export const Input = React.forwardRef(({ className = '', ...props }, ref) => (
  <input ref={ref} className={`h-9 ${sharedInputStyles} ${className}`} {...props} />
));

Input.displayName = 'Input';

export const Textarea = React.forwardRef(({ className = '', ...props }, ref) => (
  <textarea ref={ref} className={`min-h-[76px] resize-y ${sharedInputStyles} ${className}`} {...props} />
));

Textarea.displayName = 'Textarea';

export const Label = ({ className = '', children, ...props }) => (
  <label
    className={`block text-xs font-medium tracking-wide text-muted-foreground mb-1.5 ${className}`}
    {...props}
  >
    {children}
  </label>
);
