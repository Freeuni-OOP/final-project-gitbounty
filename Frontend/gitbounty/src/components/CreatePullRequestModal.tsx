import { forwardRef, useCallback, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useCreatePullRequest } from '../hooks/useCreatePullRequest';
import type { CreatePullRequestResponse } from '../services/pullRequestService';
import '../styles/CreateRepositoryModal.css';
import '../styles/CreateIssueModal.css';

interface CreatePullRequestModalProps {
    repositoryName: string;
    isOpen: boolean;
    onClose: () => void;
    onCreated: (pr: CreatePullRequestResponse) => void;
}

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
    label: string;
    error?: boolean;
}
const FormInput = forwardRef<HTMLInputElement, InputProps>(({ label, error, ...props }, ref) => (
    <div className="crm-field">
        <label className="crm-label" htmlFor={props.id}>
            {label} <span className="crm-required">*</span>
        </label>
        <input ref={ref} className={`crm-input ${error ? 'crm-input-error' : ''}`} autoComplete="off" required {...props} />
    </div>
));
FormInput.displayName = 'FormInput';

export function CreatePullRequestModal({ repositoryName, isOpen, onClose, onCreated }: Readonly<CreatePullRequestModalProps>) {
    const [form, setForm] = useState({ title: '', description: '', sourceBranch: '', targetBranch: '' });
    const [validationError, setValidationError] = useState<string | null>(null);
    const titleRef = useRef<HTMLInputElement>(null);

    const { createPullRequest, clearError, isSubmitting, error: apiError } = useCreatePullRequest();

    const updateField = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        setForm(prev => ({ ...prev, [field]: e.target.value }));
        setValidationError(null);
        clearError();
    };

    const handleClose = useCallback(() => {
        if (isSubmitting) return;
        setForm({ title: '', description: '', sourceBranch: '', targetBranch: '' });
        setValidationError(null);
        clearError();
        onClose();
    }, [isSubmitting, onClose, clearError]);

    // Focus title input on open
    useEffect(() => {
        if (isOpen) {
            const timer = setTimeout(() => titleRef.current?.focus(), 0);
            return () => clearTimeout(timer);
        }
    }, [isOpen]);

    // Handle Escape Key
    useEffect(() => {
        if (!isOpen) return;
        const handleKeyDown = (e: KeyboardEvent) => e.key === 'Escape' && handleClose();
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [handleClose, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const t = form.title.trim(), s = form.sourceBranch.trim(), tg = form.targetBranch.trim();

        if (!t) return setValidationError('Pull request title is required.');
        if (t.length > 255) return setValidationError('Pull request title must be 255 characters or fewer.');
        if (!s) return setValidationError('Source branch is required.');
        if (!tg) return setValidationError('Target branch is required.');
        if (s === tg) return setValidationError('Source and target branches must be different.');

        try {
            const createdPR = await createPullRequest(repositoryName, {
                title: t,
                sourceBranch: s,
                targetBranch: tg,
                description: form.description.trim() || null,
            });
            onCreated(createdPR);
            handleClose();
        } catch {}
    };

    const displayError = validationError ?? apiError;

    return (
        <div className="crm-backdrop" onClick={(e) => e.target === e.currentTarget && handleClose()} role="dialog" aria-modal="true" aria-labelledby="create-pr-title">
            <div className="crm-box">
                <div className="crm-header-band cim-header">
                    <div className="crm-header-icon" aria-hidden="true">
                        <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor"><path d="M1.5 3.25a2.25 2.25 0 1 1 3 2.122v5.256a2.251 2.251 0 1 1-1.5 0V5.372A2.25 2.25 0 0 1 1.5 3.25Zm5.677-.177L9.573.677A.25.25 0 0 1 10 .854V2.5h1A2.5 2.5 0 0 1 13.5 5v5.628a2.251 2.251 0 1 1-1.5 0V5a1 1 0 0 0-1-1h-1v1.646a.25.25 0 0 1-.427.177L7.177 3.427a.25.25 0 0 1 0-.354ZM3.75 2.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm0 9.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm8.25.75a.75.75 0 1 0 1.5 0 .75.75 0 0 0-1.5 0Z" /></svg>
                    </div>
                    <h2 className="crm-title" id="create-pr-title">Create a new pull request</h2>
                    <button type="button" className="crm-header-close" onClick={handleClose} disabled={isSubmitting} aria-label="Close">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.75.75 0 1 1 1.06 1.06L9.06 8l3.22 3.22a.75.75 0 1 1-1.06 1.06L8 9.06l-3.22 3.22a.75.75 0 0 1-1.06-1.06L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z" /></svg>
                    </button>
                </div>

                <form className="crm-body" onSubmit={handleSubmit}>
                    <p className="crm-subtitle">Create a new pull request to propose changes to the target branch.</p>
                    <div className="cim-codebase-card"><span className="cim-codebase-label">Codebase</span> <strong>{repositoryName}</strong></div>

                    <FormInput id="pr-title" ref={titleRef} label="Title" value={form.title} onChange={updateField('title')} placeholder="Describe the pull request briefly" maxLength={255} error={!!validationError} disabled={isSubmitting} />
                    <div className="cim-field-footer"><span>Use a clear and specific title.</span><span>{form.title.length}/255</span></div>

                    <FormInput id="pr-source-branch" label="Source Branch" value={form.sourceBranch} onChange={updateField('sourceBranch')} placeholder="e.g., feature/new-feature" disabled={isSubmitting} />
                    <FormInput id="pr-target-branch" label="Target Branch" value={form.targetBranch} onChange={updateField('targetBranch')} placeholder="e.g., main" disabled={isSubmitting} />

                    <div className="crm-field">
                        <label className="crm-label" htmlFor="pr-description">Description <span className="crm-optional">— optional</span></label>
                        <textarea id="pr-description" className="crm-textarea cim-description" value={form.description} onChange={updateField('description')} placeholder="Explain the changes..." rows={5} disabled={isSubmitting} />
                    </div>

                    {displayError && (
                        <div className="crm-error" role="alert">
                            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" className="crm-error-icon"><path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z" /></svg>
                            {displayError}
                        </div>
                    )}

                    <div className="crm-actions">
                        <button type="button" className="crm-btn-cancel" onClick={handleClose} disabled={isSubmitting}>Cancel</button>
                        <button type="submit" className="crm-btn-submit cim-submit" disabled={isSubmitting}>
                            {isSubmitting ? <><span className="crm-spinner" aria-hidden="true" />Creating…</> : 'Create pull request'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}