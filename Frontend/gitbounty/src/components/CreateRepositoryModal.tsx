import { useEffect, useRef, useState } from 'react';
import { useCreateRepository } from '../hooks/useCreateRepository';
import type { ApiRepo } from '../hooks/useRepositoriesData';
import '../styles/CreateRepositoryModal.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (repo: ApiRepo) => void;
  ownerName?: string;
}

function validate(name: string): string | null {
  const trimmed = name.trim();
  if (!trimmed) return 'Repository name is required';
  if (
    trimmed.includes('/') ||
    trimmed.includes('\\') ||
    trimmed.includes('..') ||
    trimmed.includes('.git')
  ) {
    return 'Invalid repository name format';
  }
  return null;
}

export function CreateRepositoryModal({ isOpen, onClose, onSuccess, ownerName }: Props) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);
  const nameRef = useRef<HTMLInputElement>(null);
  const { createRepository, clearError, isSubmitting, error: apiError } = useCreateRepository();

  useEffect(() => {
    if (isOpen) nameRef.current?.focus();
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') handleClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [isOpen]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!isOpen) return null;

  const handleClose = () => {
    setName('');
    setDescription('');
    setValidationError(null);
    clearError();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const err = validate(name);
    if (err) { setValidationError(err); return; }
    setValidationError(null);
    try {
      const repo = await createRepository(name, description);
      setName('');
      setDescription('');
      onSuccess(repo);
      onClose();
    } catch {
      // apiError state surfaces the message; keep modal open
    }
  };

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) handleClose();
  };

  const displayError = validationError ?? apiError;
  const previewName = name.trim() || 'repository-name';

  return (
    <div
      className="crm-backdrop"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="crm-title"
    >
      <div className="crm-box">

        {/* ── Gradient header band ── */}
        <div className="crm-header-band">
          <div className="crm-header-icon" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M2 2.5A2.5 2.5 0 0 1 4.5 0h8.75a.75.75 0 0 1 .75.75v12.5a.75.75 0 0 1-.75.75h-2.5a.75.75 0 0 1 0-1.5h1.75v-2h-8a1 1 0 0 0-.714 1.7.75.75 0 1 1-1.072 1.05A2.495 2.495 0 0 1 2 11.5Zm10.5-1h-8a1 1 0 0 0-1 1v6.708A2.486 2.486 0 0 1 4.5 9h8Z" />
            </svg>
          </div>
          <h2 className="crm-title" id="crm-title">Create a new repository</h2>
          <button className="crm-header-close" onClick={handleClose} aria-label="Close">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z" />
            </svg>
          </button>
        </div>

        {/* ── Form body ── */}
        <form className="crm-body" onSubmit={handleSubmit}>
          <p className="crm-subtitle">
            A repository contains all project files, including the revision history.
          </p>

          {/* Name row: owner slug / name input */}
          <div className="crm-field">
            <label className="crm-label" htmlFor="crm-name">
              Repository name <span className="crm-required">*</span>
            </label>
            <div className="crm-name-row">
              {ownerName && (
                <>
                  <span className="crm-owner-slug">{ownerName}</span>
                  <span className="crm-sep">/</span>
                </>
              )}
              <input
                id="crm-name"
                ref={nameRef}
                className={`crm-input crm-input-name${displayError ? ' crm-input-error' : ''}`}
                type="text"
                value={name}
                onChange={e => { setName(e.target.value); setValidationError(null); }}
                placeholder="my-repository"
                disabled={isSubmitting}
                autoComplete="off"
              />
            </div>
            {ownerName && (
              <p className="crm-preview">
                <span className="crm-preview-owner">{ownerName}</span>
                <span className="crm-preview-sep"> / </span>
                <span className={`crm-preview-repo${!name.trim() ? ' crm-preview-placeholder' : ''}`}>
                  {previewName}
                </span>
              </p>
            )}
          </div>

          <div className="crm-divider" />

          {/* Description */}
          <div className="crm-field">
            <label className="crm-label" htmlFor="crm-description">
              Description{' '}
              <span className="crm-optional">— optional</span>
            </label>
            <textarea
              id="crm-description"
              className="crm-textarea"
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="Short description of your repository"
              rows={3}
              disabled={isSubmitting}
            />
          </div>

          {/* Error */}
          {displayError && (
            <div className="crm-error" role="alert">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" className="crm-error-icon">
                <path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z" />
              </svg>
              {displayError}
            </div>
          )}

          {/* Actions */}
          <div className="crm-actions">
            <button type="button" className="crm-btn-cancel" onClick={handleClose} disabled={isSubmitting}>
              Cancel
            </button>
            <button
              type="submit"
              className="crm-btn-submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span className="crm-spinner" aria-hidden="true" />
                  Creating…
                </>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                    <path d="M2 2.5A2.5 2.5 0 0 1 4.5 0h8.75a.75.75 0 0 1 .75.75v12.5a.75.75 0 0 1-.75.75h-2.5a.75.75 0 0 1 0-1.5h1.75v-2h-8a1 1 0 0 0-.714 1.7.75.75 0 1 1-1.072 1.05A2.495 2.495 0 0 1 2 11.5Zm10.5-1h-8a1 1 0 0 0-1 1v6.708A2.486 2.486 0 0 1 4.5 9h8Z" />
                  </svg>
                  Create repository
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
