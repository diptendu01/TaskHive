import React from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { MoveHorizontalIcon, TrashIcon } from '../ui/Icons';

export const CardDetailModal = ({ card, lists, onClose, onMoveToList, onDeleteCard }) => {
  if (!card) return null;

  const currentList = lists.find((l) => l.id === card.listId);
  const otherLists = lists.filter((l) => l.id !== card.listId);

  return (
    <Modal
      open={Boolean(card)}
      onClose={onClose}
      title={card.title}
      description={currentList ? `In list: ${currentList.title}` : undefined}
    >
      <div className="space-y-5">
        <div>
          <h3 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
            Description
          </h3>
          {card.description ? (
            <p className="text-sm text-foreground/90 whitespace-pre-wrap leading-relaxed rounded-lg border border-border bg-black p-3.5">
              {card.description}
            </p>
          ) : (
            <p className="text-sm text-muted-foreground italic">No description provided.</p>
          )}
        </div>

        <div>
          <div className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2.5">
            <MoveHorizontalIcon className="h-3.5 w-3.5" />
            <span>Move to list</span>
          </div>

          {otherLists.length === 0 ? (
            <p className="text-xs text-muted-foreground">
              Add another list on this board to move cards between columns.
            </p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {otherLists.map((list) => (
                <Button
                  key={list.id}
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    onMoveToList(card.id, list.id);
                    onClose();
                  }}
                >
                  {list.title}
                </Button>
              ))}
            </div>
          )}
        </div>

        <div className="pt-3 border-t border-border flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <Badge variant="outline" className="font-mono text-[11px]">
            pos: {Math.round(card.position)}
          </Badge>
          <div className="flex items-center gap-2">
            {onDeleteCard && (
              <Button
                variant="destructive"
                size="sm"
                onClick={() => {
                  onDeleteCard(card.id);
                  onClose();
                }}
              >
                <TrashIcon className="h-3.5 w-3.5" />
                <span>Delete Card</span>
              </Button>
            )}
            <Button variant="secondary" size="sm" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
};
