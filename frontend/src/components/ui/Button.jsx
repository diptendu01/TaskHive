import React from 'react';

const VARIANTS = {
  primary:
    'bg-primary text-primary-foreground hover:bg-primary/90 active:bg-primary/80 border border-transparent',
  secondary:
    'bg-secondary text-secondary-foreground hover:bg-secondary/80 border border-border',
  outline:
    'bg-transparent text-foreground border border-border hover:bg-secondary hover:border-zinc-600',
  ghost:
    'bg-transparent text-muted-foreground hover:text-foreground hover:bg-secondary border border-transparent',
  accent:
    'bg-accent text-accent-foreground hover:bg-accent/90 border border-transparent font-semibold',
  destructive:
    'bg-destructive text-destructive-foreground hover:bg-destructive/90 border border-transparent',
};

const SIZES = {
  sm: 'h-8 px-3 text-xs gap-1.5 rounded-md',
  md: 'h-9 px-4 text-sm gap-2 rounded-md',
  lg: 'h-10 px-5 text-sm gap-2 rounded-lg',
  icon: 'h-8 w-8 p-0 rounded-md',
};

export const Button = React.forwardRef(
  ({ className = '', variant = 'primary', size = 'md', type = 'button', ...props }, ref) => (
    <button
      ref={ref}
      type={type}
      className={[
        'inline-flex items-center justify-center font-medium whitespace-nowrap cursor-pointer',
        'transition-colors duration-150 select-none',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
        'disabled:pointer-events-none disabled:opacity-45',
        VARIANTS[variant] || VARIANTS.primary,
        SIZES[size] || SIZES.md,
        className,
      ].join(' ')}
      {...props}
    />
  )
);

Button.displayName = 'Button';
