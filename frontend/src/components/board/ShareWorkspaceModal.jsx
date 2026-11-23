import React, { useEffect, useState } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input, Label } from '../ui/Input';
import { Badge } from '../ui/Badge';
import { Alert } from '../ui/Alert';
import { TrashIcon } from '../ui/Icons';
import { api } from '../../services/api';

export const ShareWorkspaceModal = ({
  open,
  workspaceId,
  token,
  currentUserId,
  onUnauthorized,
  onClose,
}) => {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [email, setEmail] = useState('');
  const [inviting, setInviting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (!open || !workspaceId || !token) return;
    let cancelled = false;
    setLoading(true);
    setError('');
    setSuccess('');

    api
      .getWorkspaceMembers(token, workspaceId, { onUnauthorized })
      .then((data) => {
        if (!cancelled && Array.isArray(data)) {
          setMembers(data);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load workspace members');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [open, workspaceId, token, onUnauthorized]);

  const handleInvite = async (e) => {
    e.preventDefault();
    const trimmed = email.trim();
    if (!trimmed || inviting || !workspaceId) return;
    setInviting(true);
    setError('');
    setSuccess('');

    try {
      const added = await api.inviteWorkspaceMember(
        token,
        workspaceId,
        { email: trimmed, role: 'MEMBER' },
        { onUnauthorized }
      );
      setMembers((prev) => [...prev, added]);
      setEmail('');
      setSuccess(`Invited ${added.email} to collaborate on this workspace.`);
    } catch (err) {
      setError(err.message || 'Failed to invite member');
    } finally {
      setInviting(false);
    }
  };

  const handleRemove = async (memberId) => {
    setError('');
    setSuccess('');
    try {
      await api.removeWorkspaceMember(token, workspaceId, memberId, { onUnauthorized });
      setMembers((prev) => prev.filter((m) => m.id !== memberId));
    } catch (err) {
      setError(err.message || 'Failed to remove member');
    }
  };

  const isOwner = members.some(
    (m) => m.userId === currentUserId && m.role === 'OWNER'
  );

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Share Workspace"
      description="Invite registered TaskHive users by email to collaborate on this board in real time."
    >
      <div className="space-y-5">
        {error && <Alert variant="destructive">{error}</Alert>}
        {success && (
          <div className="rounded-lg border border-accent/40 bg-accent/10 px-3 py-2 text-xs text-accent">
            {success}
          </div>
        )}

        <form onSubmit={handleInvite} className="space-y-2">
          <Label htmlFor="invite-member-email">Collaborator email</Label>
          <div className="flex items-center gap-2">
            <Input
              id="invite-member-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="teammate@example.com"
              required
              className="bg-black flex-1"
            />
            <Button type="submit" size="sm" disabled={inviting || !email.trim()}>
              {inviting ? 'Inviting...' : 'Invite'}
            </Button>
          </div>
        </form>

        <div className="space-y-2">
          <h3 className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
            Members ({members.length})
          </h3>
          {loading ? (
            <p className="text-xs text-muted-foreground">Loading members...</p>
          ) : (
            <ul className="divide-y divide-border rounded-lg border border-border bg-black">
              {members.map((member) => (
                <li
                  key={member.id}
                  className="flex items-center justify-between gap-2 px-3 py-2.5"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">
                      {member.name}
                    </p>
                    <p className="text-xs text-muted-foreground truncate">
                      {member.email}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <Badge variant="outline" className="font-mono text-[10px]">
                      {member.role}
                    </Badge>
                    {isOwner && member.userId !== currentUserId && (
                      <button
                        type="button"
                        onClick={() => handleRemove(member.id)}
                        aria-label={`Remove ${member.name}`}
                        title="Remove member"
                        className="rounded p-1 text-muted-foreground hover:text-red-400 hover:bg-red-950/60 transition-colors cursor-pointer"
                      >
                        <TrashIcon className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </Modal>
  );
};
