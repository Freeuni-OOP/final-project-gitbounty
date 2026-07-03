import { useEffect, useRef, useState } from 'react';
import {
    type CodebaseMember,
    type CodebaseRole,
    useAddCodebaseMember,
} from '../hooks/useAddCodebaseMember';
import '../styles/CreateRepositoryModal.css';

interface Props {
    isOpen: boolean;
    repositoryName: string;
    onClose: () => void;
    onSuccess: (member: CodebaseMember) => void;
}

const ROLES: CodebaseRole[] = ['DEVELOPER', 'MAINTAINER', 'REPORTER'];

function validate(username: string): string | null {
    if (!username.trim()) return 'Username is required';
    return null;
}

export function AddCodebaseMemberModal({
                                           isOpen,
                                           repositoryName,
                                           onClose,
                                           onSuccess,
                                       }: Readonly<Props>) {
    const [username, setUsername] = useState('');
    const [role, setRole] = useState<CodebaseRole>('DEVELOPER');
    const [validationError, setValidationError] = useState<string | null>(null);
    const usernameRef = useRef<HTMLInputElement>(null);
    const { addMember, clearError, isSubmitting, error: apiError } = useAddCodebaseMember();

    useEffect(() => {
        if (isOpen) usernameRef.current?.focus();
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen) return;

        const handler = (e: KeyboardEvent) => {
            if (e.key === 'Escape') handleClose();
        };

        document.addEventListener('keydown', handler);
        return () => document.removeEventListener('keydown', handler);
    }, [isOpen]); // eslint-disable-line react-hooks/exhaustive-deps

    if (!isOpen) return null;

    const handleClose = () => {
        setUsername('');
        setRole('DEVELOPER');
        setValidationError(null);
        clearError();
        onClose();
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        const err = validate(username);
        if (err) {
            setValidationError(err);
            return;
        }

        setValidationError(null);

        try {
            const member = await addMember(repositoryName, username, role);
            setUsername('');
            setRole('DEVELOPER');
            onSuccess(member);
            onClose();
        } catch {
            // apiError state surfaces the message; keep modal open
        }
    };

    const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
        if (e.target === e.currentTarget) handleClose();
    };

    const displayError = validationError ?? apiError;

    return (
        <div
            className="crm-backdrop"
            onClick={handleBackdropClick}
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-member-title"
        >
            <div className="crm-box">
                <div className="crm-header-band">
                    <div className="crm-header-icon" aria-hidden="true">
                        <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
                            <path d="M7.5 8a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Zm-5 6.5c0-2.33 2.67-4 5-4s5 1.67 5 4a.5.5 0 0 1-.5.5H3a.5.5 0 0 1-.5-.5Zm10.5-7a.75.75 0 0 1 .75.75V10h1.75a.75.75 0 0 1 0 1.5h-1.75v1.75a.75.75 0 0 1-1.5 0V11.5H10.5a.75.75 0 0 1 0-1.5h1.75V8.25A.75.75 0 0 1 13 7.5Z" />
                        </svg>
                    </div>
                    <h2 className="crm-title" id="add-member-title">Add codebase member</h2>
                    <button className="crm-header-close" onClick={handleClose} aria-label="Close">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                            <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z" />
                        </svg>
                    </button>
                </div>

                <form className="crm-body" onSubmit={handleSubmit}>
                    <p className="crm-subtitle">
                        Add an existing GitBounty user to <strong>{repositoryName}</strong> and choose their role.
                    </p>

                    <div className="crm-field">
                        <label className="crm-label" htmlFor="add-member-username">
                            Username <span className="crm-required">*</span>
                        </label>
                        <input
                            id="add-member-username"
                            ref={usernameRef}
                            className={`crm-input${displayError ? ' crm-input-error' : ''}`}
                            type="text"
                            value={username}
                            onChange={e => {
                                setUsername(e.target.value);
                                setValidationError(null);
                            }}
                            placeholder="username"
                            disabled={isSubmitting}
                            autoComplete="off"
                        />
                    </div>

                    <div className="crm-field">
                        <label className="crm-label" htmlFor="add-member-role">
                            Role
                        </label>
                        <select
                            id="add-member-role"
                            className="crm-input"
                            value={role}
                            onChange={e => setRole(e.target.value as CodebaseRole)}
                            disabled={isSubmitting}
                        >
                            {ROLES.map(roleOption => (
                                <option key={roleOption} value={roleOption}>
                                    {roleOption}
                                </option>
                            ))}
                        </select>
                    </div>

                    {displayError && (
                        <div className="crm-error" role="alert">
                            {displayError}
                        </div>
                    )}

                    <div className="crm-actions">
                        <button type="button" className="crm-btn-cancel" onClick={handleClose} disabled={isSubmitting}>
                            Cancel
                        </button>
                        <button type="submit" className="crm-btn-submit" disabled={isSubmitting}>
                            {isSubmitting ? 'Adding…' : 'Add member'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}