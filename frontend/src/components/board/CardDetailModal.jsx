import React, { useEffect, useState } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Input, Label, Textarea } from '../ui/Input';
import { MoveHorizontalIcon, TrashIcon } from '../ui/Icons';

export const CardDetailModal = ({
  card,
  lists,
  onClose,
  onUpdateCard,
  onMoveToList,
  onDeleteCard,
}) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (card) {
      setTitle(card.title || '');
      setDescription(card.description || '');
    }
  }, [card]);

  if (!card) return null;

  const currentList = lists.find((l) => l.id === card.listId);
  const otherLists = lists.filter((l) => l.id !== card.listId);
  const hasChanges =
    title.trim() !== (card.title || '') ||
    description.trim() !== (card.description || '');

  const handleSave = async (e) => {
    e.preventDefault();
    const trimmedTitle = title.trim();
    if (!trimmedTitle || saving || !onUpdateCard) return;
    setSaving(true);
    try {
      const ok = await onUpdateCard(card.id, {
        title: trimmedTitle,
        description: description.trim() || null,
      });
      if (ok !== false) {
        onClose();
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={Boolean(card)}
      onClose={onClose}
      title="Edit Card"
      description={currentList ? `In list: ${currentList.title}` : undefined}
    >
      <form onSubmit={handleSave} className="space-y-5">
        <div className="space-y-1.5">
          <Label htmlFor="edit-card-title">Title</Label>
          <Input
            id="edit-card-title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            maxLength={512}
            className="bg-black"
          />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="edit-card-description">Description</Label>
          <Textarea
            id="edit-card-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Add details, acceptance criteria, or notes..."
            rows={4}
            maxLength={4096}
            className="bg-black"
          />
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
                  type="button"
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
            pos: {Number(card.position).toFixed(2)}
          </Badge>
          <div className="flex items-center gap-2">
            {onDeleteCard && (
              <Button
                type="button"
                variant="destructive"
                size="sm"
                onClick={() => {
                  onDeleteCard(card.id);
                  onClose();
                }}
              >
                <TrashIcon className="h-3.5 w-3.5" />
                <span>Delete</span>
              </Button>
            )}
            <Button type="button" variant="secondary" size="sm" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" size="sm" disabled={saving || !hasChanges || !title.trim()}>
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  );
};
