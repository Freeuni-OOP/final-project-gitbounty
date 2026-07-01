import {
    useCallback,
    useEffect,
    useRef,
    useState,
} from 'react';

import type {
    FormEvent,
    MouseEvent as ReactMouseEvent,
} from 'react';

import { useCreateIssue } from '../hooks/useCreateIssue';
import type { IssueAPI } from '../types/Issue';

import '../styles/CreateRepositoryModal.css';
import '../styles/CreateIssueModal.css';

interface CreateIssueModalProps {
    repositoryName: string;
    isOpen: boolean;
    onClose: () => void;
    onCreated: (issue: IssueAPI) => void;
}

export function CreateIssueModal({
                                     repositoryName,
                                     isOpen,
                                     onClose,
                                     onCreated,
                                 }: CreateIssueModalProps) {
    const [title, setTitle] = useState('');
    const [description, setDescription] =
        useState('');

    const [validationError, setValidationError] =
        useState<string | null>(null);

    const titleRef = useRef<HTMLInputElement>(null);

    const {
        createIssue,
        clearError,
        isSubmitting,
        error: apiError,
    } = useCreateIssue();

    const resetForm = useCallback(() => {
        setTitle('');
        setDescription('');
        setValidationError(null);
        clearError();
    }, [clearError]);

    const handleClose = useCallback(() => {
        if (isSubmitting) {
            return;
        }

        resetForm();
        onClose();
    }, [
        isSubmitting,
        onClose,
        resetForm,
    ]);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        const focusTimer = window.setTimeout(() => {
            titleRef.current?.focus();
        }, 0);

        return () => {
            window.clearTimeout(focusTimer);
        };
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        const handleKeyDown = (
            event: KeyboardEvent
        ) => {
            if (
                event.key === 'Escape' &&
                !isSubmitting
            ) {
                handleClose();
            }
        };

        document.addEventListener(
            'keydown',
            handleKeyDown
        );

        return () => {
            document.removeEventListener(
                'keydown',
                handleKeyDown
            );
        };
    }, [
        handleClose,
        isOpen,
        isSubmitting,
    ]);

    if (!isOpen) {
        return null;
    }

    const handleSubmit = async (
        event: FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        const normalizedTitle = title.trim();

        if (!normalizedTitle) {
            setValidationError(
                'Issue title is required.'
            );
            return;
        }

        if (normalizedTitle.length > 255) {
            setValidationError(
                'Issue title must be 255 characters or fewer.'
            );
            return;
        }

        setValidationError(null);

        try {
            const createdIssue =
                await createIssue(
                    repositoryName,
                    {
                        title: normalizedTitle,
                        description:
                            description.trim() || null,
                    }
                );

            onCreated(createdIssue);
            resetForm();
            onClose();
        } catch {
            // The hook exposes the API error.
        }
    };

    const handleBackdropClick = (
        event: ReactMouseEvent<HTMLDivElement>
    ) => {
        if (
            event.target === event.currentTarget &&
            !isSubmitting
        ) {
            handleClose();
        }
    };

    const displayError =
        validationError ?? apiError;

    return (
        <div
            className="crm-backdrop"
            onClick={handleBackdropClick}
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-issue-title"
        >
            <div className="crm-box">
                <div className="crm-header-band cim-header">
                    <div
                        className="crm-header-icon"
                        aria-hidden="true"
                    >
                        <svg
                            width="18"
                            height="18"
                            viewBox="0 0 16 16"
                            fill="currentColor"
                        >
                            <path d="M8 9.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z" />
                            <path d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0ZM1.5 8a6.5 6.5 0 1 0 13 0 6.5 6.5 0 0 0-13 0Z" />
                        </svg>
                    </div>

                    <h2
                        className="crm-title"
                        id="create-issue-title"
                    >
                        Create a new issue
                    </h2>

                    <button
                        type="button"
                        className="crm-header-close"
                        onClick={handleClose}
                        disabled={isSubmitting}
                        aria-label="Close"
                    >
                        <svg
                            width="16"
                            height="16"
                            viewBox="0 0 16 16"
                            fill="currentColor"
                        >
                            <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.75.75 0 1 1 1.06 1.06L9.06 8l3.22 3.22a.75.75 0 1 1-1.06 1.06L8 9.06l-3.22 3.22a.75.75 0 0 1-1.06-1.06L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z" />
                        </svg>
                    </button>
                </div>

                <form
                    className="crm-body"
                    onSubmit={handleSubmit}
                >
                    <p className="crm-subtitle">
                        Report a bug, request a feature, or
                        describe work that should be completed.
                    </p>

                    <div className="cim-codebase-card">
                        <span className="cim-codebase-label">
                            Codebase
                        </span>

                        <strong>
                            {repositoryName}
                        </strong>
                    </div>

                    <div className="crm-field">
                        <label
                            className="crm-label"
                            htmlFor="issue-title"
                        >
                            Title{' '}
                            <span className="crm-required">
                                *
                            </span>
                        </label>

                        <input
                            id="issue-title"
                            ref={titleRef}
                            className={`crm-input ${
                                validationError
                                    ? 'crm-input-error'
                                    : ''
                            }`}
                            type="text"
                            value={title}
                            onChange={(event) => {
                                setTitle(
                                    event.target.value
                                );
                                setValidationError(null);
                                clearError();
                            }}
                            placeholder="Describe the issue briefly"
                            maxLength={255}
                            disabled={isSubmitting}
                            autoComplete="off"
                            required
                        />

                        <div className="cim-field-footer">
                            <span>
                                Use a clear and specific title.
                            </span>

                            <span>
                                {title.length}/255
                            </span>
                        </div>
                    </div>

                    <div className="crm-field">
                        <label
                            className="crm-label"
                            htmlFor="issue-description"
                        >
                            Description{' '}
                            <span className="crm-optional">
                                — optional
                            </span>
                        </label>

                        <textarea
                            id="issue-description"
                            className="crm-textarea cim-description"
                            value={description}
                            onChange={(event) => {
                                setDescription(
                                    event.target.value
                                );
                            }}
                            placeholder={
                                'Explain the problem, expected behavior, or acceptance criteria.'
                            }
                            rows={7}
                            disabled={isSubmitting}
                        />
                    </div>

                    {displayError && (
                        <div
                            className="crm-error"
                            role="alert"
                        >
                            <svg
                                width="16"
                                height="16"
                                viewBox="0 0 16 16"
                                fill="currentColor"
                                className="crm-error-icon"
                            >
                                <path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z" />
                            </svg>

                            {displayError}
                        </div>
                    )}

                    <div className="crm-actions">
                        <button
                            type="button"
                            className="crm-btn-cancel"
                            onClick={handleClose}
                            disabled={isSubmitting}
                        >
                            Cancel
                        </button>

                        <button
                            type="submit"
                            className="crm-btn-submit cim-submit"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? (
                                <>
                                    <span
                                        className="crm-spinner"
                                        aria-hidden="true"
                                    />
                                    Creating…
                                </>
                            ) : (
                                <>Create issue</>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}