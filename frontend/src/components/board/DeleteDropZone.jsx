import React from 'react';
import { useDroppable } from '@dnd-kit/core';
import { TrashIcon } from '../ui/Icons';

export const DELETE_DROP_ZONE_ID = '__delete-card-zone__';

export const DeleteDropZone = ({ visible }) => {
  const { setNodeRef, isOver } = useDroppable({
    id: DELETE_DROP_ZONE_ID,
    disabled: !visible,
  });

  if (!visible) return null;

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 px-4 w-full max-w-sm">
      <div
        ref={setNodeRef}
        role="region"
        aria-label="Drop card here to delete"
        className={[
          'flex items-center justify-center gap-2.5 rounded-xl border-2 border-dashed px-5 py-4 text-sm font-medium backdrop-blur-md transition-colors duration-150 shadow-2xl',
          isOver
            ? 'border-red-500 bg-red-950/90 text-red-100 ring-2 ring-red-500/40'
            : 'border-red-500/45 bg-surface/95 text-red-300',
        ].join(' ')}
      >
        <TrashIcon className="h-4 w-4 shrink-0" />
        <span>{isOver ? 'Release to delete card' : 'Drop here to delete card'}</span>
      </div>
    </div>
  );
};
