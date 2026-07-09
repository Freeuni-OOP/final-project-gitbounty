import { useEffect, useRef, useState } from 'react';
import { useDeleteRepository } from '../hooks/useDeleteRepository';
import '../styles/CreateRepositoryModal.css';

interface Props {
  isOpen: boolean;
  repositoryId: number;
  repositoryName: string;
  onClose: () => void;
  onDeleted: () => void;
}

export function DeleteRepositoryModal({ isOpen, repositoryId, repositoryName, onClose, onDeleted }: Props) {
  const [confirmText, setConfirmText] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const { deleteRepository, clearError, isSubmitting, error: apiError } = useDeleteRepository();

  useEffect(() => {
    if (isOpen) inputRef.current?.focus();
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') handleClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [isOpen]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!isOpen) return null;

  const handleClose = () => {
    setConfirmText('');
    clearError();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (confirmText !== repositoryName) return;

    try {
      await deleteRepository(repositoryId);
      onDeleted();
    } catch {
      // apiError state surfaces the message; keep modal open
    }
  };

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) handleClose();
  };

  const canDelete = confirmText === repositoryName && !isSubmitting;

  return (
    <div
      className="crm-backdrop"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-repo-title"
    >
      <div className="crm-box">
        <div className="crm-header-band crm-header-band-danger">
          <div className="crm-header-icon" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z" />
            </svg>
          </div>
          <h2 className="crm-title" id="delete-repo-title">Delete repository</h2>
          <button className="crm-header-close" onClick={handleClose} aria-label="Close">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z" />
            </svg>
          </button>
        </div>

        <form className="crm-body" onSubmit={handleSubmit}>
          <div className="crm-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" className="crm-error-icon">
              <path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z" />
            </svg>
            <span>
              This action <strong>cannot be undone</strong>. This will permanently delete the{' '}
              <strong>{repositoryName}</strong> repository, all of its issues, pull requests, and bounties.
              Any escrowed bounty funds will be refunded to their owner.
            </span>
          </div>

          <div className="crm-field">
            <label className="crm-label" htmlFor="delete-repo-confirm">
              Please type <strong>{repositoryName}</strong> to confirm.
            </label>
            <input
              id="delete-repo-confirm"
              ref={inputRef}
              className="crm-input"
              type="text"
              value={confirmText}
              onChange={e => setConfirmText(e.target.value)}
              placeholder={repositoryName}
              disabled={isSubmitting}
              autoComplete="off"
              spellCheck={false}
            />
          </div>

          {apiError && (
            <div className="crm-error" role="alert">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" className="crm-error-icon">
                <path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z" />
              </svg>
              {apiError}
            </div>
          )}

          <div className="crm-actions">
            <button type="button" className="crm-btn-cancel" onClick={handleClose} disabled={isSubmitting}>
              Cancel
            </button>
            <button type="submit" className="crm-btn-submit crm-btn-danger" disabled={!canDelete}>
              {isSubmitting ? (
                <>
                  <span className="crm-spinner" aria-hidden="true" />
                  Deleting…
                </>
              ) : (
                'I understand, delete this repository'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
