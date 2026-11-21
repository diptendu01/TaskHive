import React from 'react';

const baseProps = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
};

export const KanbanIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <rect x="3" y="3" width="18" height="18" rx="3" />
    <path d="M9 7v10" />
    <path d="M15 7v6" />
  </svg>
);

export const PlusIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="M12 5v14" />
    <path d="M5 12h14" />
  </svg>
);

export const ArrowLeftIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="m12 19-7-7 7-7" />
    <path d="M19 12H5" />
  </svg>
);

export const ArrowUpRightIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="M7 17 17 7" />
    <path d="M7 7h10v10" />
  </svg>
);

export const LogOutIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
    <polyline points="16 17 21 12 16 7" />
    <line x1="21" y1="12" x2="9" y2="12" />
  </svg>
);

export const SearchIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <circle cx="11" cy="11" r="8" />
    <path d="m21 21-4.3-4.3" />
  </svg>
);

export const GripVerticalIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <circle cx="9" cy="6" r="1" fill="currentColor" stroke="none" />
    <circle cx="9" cy="12" r="1" fill="currentColor" stroke="none" />
    <circle cx="9" cy="18" r="1" fill="currentColor" stroke="none" />
    <circle cx="15" cy="6" r="1" fill="currentColor" stroke="none" />
    <circle cx="15" cy="12" r="1" fill="currentColor" stroke="none" />
    <circle cx="15" cy="18" r="1" fill="currentColor" stroke="none" />
  </svg>
);

export const MoveHorizontalIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="m18 8 4 4-4 4" />
    <path d="m6 8-4 4 4 4" />
    <path d="M2 12h20" />
  </svg>
);

export const TrashIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="M3 6h18" />
    <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
    <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
    <line x1="10" y1="11" x2="10" y2="17" />
    <line x1="14" y1="11" x2="14" y2="17" />
  </svg>
);

export const XIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="M18 6 6 18" />
    <path d="m6 6 12 12" />
  </svg>
);

export const AlertCircleIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <circle cx="12" cy="12" r="10" />
    <line x1="12" y1="8" x2="12" y2="12" />
    <line x1="12" y1="16" x2="12.01" y2="16" />
  </svg>
);

export const EyeIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
);

export const EyeOffIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
    <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
    <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
    <line x1="2" y1="2" x2="22" y2="22" />
  </svg>
);

export const AlignLeftIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <line x1="21" y1="6" x2="3" y2="6" />
    <line x1="15" y1="12" x2="3" y2="12" />
    <line x1="17" y1="18" x2="3" y2="18" />
  </svg>
);

export const LayersIcon = ({ className = 'h-4 w-4', ...props }) => (
  <svg {...baseProps} className={className} aria-hidden="true" {...props}>
    <polygon points="12 2 2 7 12 12 22 7 12 2" />
    <polyline points="2 17 12 22 22 17" />
    <polyline points="2 12 12 17 22 12" />
  </svg>
);
