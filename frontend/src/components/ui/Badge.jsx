import React from 'react';

const BADGE_VARIANTS = {
  default: 'bg-secondary text-foreground border-border',
  outline: 'bg-transparent text-muted-foreground border-border',
  success: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
  warning: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
  destructive: 'bg-red-500/10 text-red-400 border-red-500/30',
  accent: 'bg-teal-500/10 text-teal-300 border-teal-500/30',
};

export const Badge = ({ variant = 'default', className = '', children, ...props }) => (
  <span
    className={[
      'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium whitespace-nowrap',
      BADGE_VARIANTS[variant] || BADGE_VARIANTS.default,
      className,
    ].join(' ')}
    {...props}
  >
    {children}
  </span>
);
