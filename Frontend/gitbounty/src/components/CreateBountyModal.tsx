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
import { useCreateBounty } from '../hooks/useCreateBounty';
import { useBalance } from '../context/BalanceContext';
import type { BountyAPI } from '../types/Bounty';
import '../styles/CreateRepositoryModal.css';
import '../styles/CreateBountyModal.css';

export interface BountyIssue {
    id: number;
    number?: number;
    title: string;
}

interface CreateBountyModalProps {
    issue: BountyIssue | null;
    isOpen: boolean;
    onClose: () => void;
    onCreated: (bounty: BountyAPI) => void;
}

export function CreateBountyModal({
                                      issue,
                                      isOpen,
                                      onClose,
                                      onCreated,
                                  }: CreateBountyModalProps) {
    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [amount, setAmount] = useState('');
    const [validationError, setValidationError] =
        useState<string | null>(null);

    const amountRef = useRef<HTMLInputElement>(null);

    const {
        createBounty,
        clearError,
        isSubmitting,
        error: apiError,
    } = useCreateBounty();

    const {
        balance,
        refreshBalance,
    } = useBalance();

    const resetForm = useCallback(() => {
        setTitle('');
        setDescription('');
        setAmount('');
        setValidationError(null);
        clearError();
    }, [clearError]);

    const handleClose = useCallback(() => {
        resetForm();
        onClose();
    }, [onClose, resetForm]);

    useEffect(() => {
        if (!isOpen || !issue) {
            return;
        }

        setTitle(issue.title);
        setDescription('');
        setAmount('');
        setValidationError(null);
        clearError();
        refreshBalance();

        const focusTimer = window.setTimeout(() => {
            amountRef.current?.focus();
        }, 0);

        return () => {
            window.clearTimeout(focusTimer);
        };
    }, [
        isOpen,
        issue,
        clearError,
        refreshBalance,
    ]);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                handleClose();
            }
        };

        document.addEventListener('keydown', handleKeyDown);

        return () => {
            document.removeEventListener(
                'keydown',
                handleKeyDown
            );
        };
    }, [isOpen, handleClose]);

    if (!isOpen || !issue) {
        return null;
    }

    const handleSubmit = async (
        event: FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        const normalizedTitle = title.trim();
        const parsedAmount = Number(amount);

        if (!normalizedTitle) {
            setValidationError('Bounty title is required.');
            return;
        }

        if (
            !Number.isFinite(parsedAmount) ||
            parsedAmount <= 0
        ) {
            setValidationError(
                'Bounty amount must be greater than zero.'
            );
            return;
        }

        setValidationError(null);

        try {
            const createdBounty = await createBounty({
                issueId: issue.id,
                title: normalizedTitle,
                description:
                    description.trim() || null,
                amount: parsedAmount,
            });

            onCreated(createdBounty);
            refreshBalance();
            handleClose();
        } catch {
            // The hook exposes the server message.
        }
    };

    const handleBackdropClick = (
        event: ReactMouseEvent<HTMLDivElement>
    ) => {
        if (event.target === event.currentTarget) {
            handleClose();
        }
    };

    const displayError = validationError ?? apiError;
    const issueNumber = issue.number ?? issue.id;

    return (
        <div
            className="crm-backdrop"
            onClick={handleBackdropClick}
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-bounty-title"
        >
            <div className="crm-box">
                <div className="crm-header-band">
                    <div
                        className="crm-header-icon"
                        aria-hidden="true"
                    >
                        <strong>$</strong>
                    </div>

                    <h2
                        className="crm-title"
                        id="create-bounty-title"
                    >
                        Create bounty
                    </h2>

                    <button
                        type="button"
                        className="crm-header-close"
                        onClick={handleClose}
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
                        Fund this issue with credits from your
                        current balance. The credits will be held
                        until the bounty payout is approved.
                    </p>

                    <div className="cbm-issue-card">
                        <span className="cbm-issue-number">
                            Issue #{issueNumber}
                        </span>

                        <strong className="cbm-issue-title">
                            {issue.title}
                        </strong>
                    </div>

                    <div className="cbm-balance-card">
                        <span>Available balance</span>
                        <strong>
                            {balance.toLocaleString()} credits
                        </strong>
                    </div>

                    <div className="crm-field">
                        <label
                            className="crm-label"
                            htmlFor="bounty-title"
                        >
                            Bounty title{' '}
                            <span className="crm-required">
                                *
                            </span>
                        </label>

                        <input
                            id="bounty-title"
                            className="crm-input"
                            type="text"
                            value={title}
                            onChange={(event) => {
                                setTitle(event.target.value);
                                setValidationError(null);
                            }}
                            maxLength={255}
                            disabled={isSubmitting}
                            required
                        />
                    </div>

                    <div className="crm-field">
                        <label
                            className="crm-label"
                            htmlFor="bounty-description"
                        >
                            Description{' '}
                            <span className="crm-optional">
                                — optional
                            </span>
                        </label>

                        <textarea
                            id="bounty-description"
                            className="crm-textarea"
                            value={description}
                            onChange={(event) => {
                                setDescription(event.target.value);
                            }}
                            placeholder="Describe the expected result or reward conditions"
                            rows={4}
                            disabled={isSubmitting}
                        />
                    </div>

                    <div className="crm-field">
                        <label
                            className="crm-label"
                            htmlFor="bounty-amount"
                        >
                            Reward amount{' '}
                            <span className="crm-required">
                                *
                            </span>
                        </label>

                        <div className="cbm-amount-input-wrapper">
                            <input
                                id="bounty-amount"
                                ref={amountRef}
                                className="crm-input cbm-amount-input"
                                type="number"
                                min="0.01"
                                step="0.01"
                                inputMode="decimal"
                                value={amount}
                                onChange={(event) => {
                                    setAmount(event.target.value);
                                    setValidationError(null);
                                }}
                                placeholder="100"
                                disabled={isSubmitting}
                                required
                            />

                            <span className="cbm-amount-unit">
                                credits
                            </span>
                        </div>

                        <p className="cbm-help-text">
                            This amount will be deducted immediately
                            and placed into escrow.
                        </p>
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
                            className="crm-btn-submit"
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
                                <>Create bounty</>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}