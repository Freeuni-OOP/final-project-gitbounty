import {
    useEffect,
    useMemo,
    useState,
} from 'react';

import { bountyApi } from '../services/bountyService';
import { issueApi } from '../services/issueService';

import { CreateBountyModal } from './CreateBountyModal';
import { CreateIssueModal } from './CreateIssueModal';

import type { BountyAPI } from '../types/Bounty';
import type { IssueAPI } from '../types/Issue';

import '../styles/RepoTabs.css';

interface IssuesTabProps {
    repoId: number;
    repoName: string;
    canCreateIssues: boolean;
    canCreateBounties: boolean;
}

type Filter = 'open' | 'closed';

function OpenIcon() {
    return (
        <svg
            className="issue-status-icon open"
            viewBox="0 0 16 16"
            aria-hidden="true"
        >
            <path d="M8 9.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z" />
            <path d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0ZM1.5 8a6.5 6.5 0 1 0 13 0 6.5 6.5 0 0 0-13 0Z" />
        </svg>
    );
}

function ClosedIcon() {
    return (
        <svg
            className="issue-status-icon closed"
            viewBox="0 0 16 16"
            aria-hidden="true"
        >
            <path d="M11.28 6.78a.75.75 0 0 0-1.06-1.06L7.25 8.69 5.78 7.22a.75.75 0 0 0-1.06 1.06l2 2a.75.75 0 0 0 1.06 0l3.5-3.5Z" />
            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0Zm-1.5 0a6.5 6.5 0 1 0-13 0 6.5 6.5 0 0 0 13 0Z" />
        </svg>
    );
}

function isClosed(issue: IssueAPI): boolean {
    return issue.status === 'CLOSED';
}

function formatDate(date: string): string {
    return new Date(date).toLocaleDateString(
        undefined,
        {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
        }
    );
}

export default function IssuesTab({
                                      repoId,
                                      repoName,
                                      canCreateIssues,
                                      canCreateBounties,
                                  }: IssuesTabProps) {
    const [filter, setFilter] =
        useState<Filter>('open');

    const [issues, setIssues] =
        useState<IssueAPI[]>([]);

    const [bounties, setBounties] =
        useState<BountyAPI[]>([]);

    const [selectedIssue, setSelectedIssue] =
        useState<IssueAPI | null>(null);

    const [
        isCreateIssueOpen,
        setIsCreateIssueOpen,
    ] = useState(false);

    const [successMessage, setSuccessMessage] =
        useState<string | null>(null);

    const [loading, setLoading] =
        useState(true);

    const [error, setError] =
        useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function loadIssuesAndBounties() {
            try {
                setLoading(true);
                setError(null);

                const [
                    repositoryIssues,
                    repositoryBounties,
                ] = await Promise.all([
                    issueApi.getIssues(repoName),
                    bountyApi.getBountiesByRepo(
                        repoId.toString()
                    ),
                ]);

                if (!cancelled) {
                    setIssues(repositoryIssues);
                    setBounties(repositoryBounties);
                }
            } catch {
                if (!cancelled) {
                    setError(
                        'Failed to load issues and bounties.'
                    );
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadIssuesAndBounties();

        return () => {
            cancelled = true;
        };
    }, [repoId, repoName]);

    useEffect(() => {
        if (!successMessage) {
            return;
        }

        const timer = window.setTimeout(() => {
            setSuccessMessage(null);
        }, 4000);

        return () => {
            window.clearTimeout(timer);
        };
    }, [successMessage]);

    const sortedIssues = useMemo(
        () =>
            [...issues].sort(
                (first, second) =>
                    second.number - first.number
            ),
        [issues]
    );

    const bountyByIssueId = useMemo(
        () =>
            new Map<number, BountyAPI>(
                bounties.map((bounty) => [
                    bounty.issueId,
                    bounty,
                ])
            ),
        [bounties]
    );

    const openIssues = sortedIssues.filter(
        (issue) => !isClosed(issue)
    );

    const closedIssues = sortedIssues.filter(
        isClosed
    );

    const shownIssues =
        filter === 'open'
            ? openIssues
            : closedIssues;

    const handleIssueCreated = (
        createdIssue: IssueAPI
    ) => {
        setIssues((currentIssues) => [
            createdIssue,
            ...currentIssues.filter(
                (issue) =>
                    issue.id !== createdIssue.id
            ),
        ]);

        setFilter('open');
        setIsCreateIssueOpen(false);

        setSuccessMessage(
            `Issue #${createdIssue.number} was created successfully.`
        );
    };

    const handleBountyCreated = (
        createdBounty: BountyAPI
    ) => {
        setBounties((currentBounties) => [
            ...currentBounties.filter(
                (bounty) =>
                    bounty.id !== createdBounty.id
            ),
            createdBounty,
        ]);

        setSelectedIssue(null);

        setSuccessMessage(
            `Created a ${createdBounty.amount.toLocaleString()} credit bounty for "${createdBounty.title}".`
        );
    };

    if (loading) {
        return (
            <div className="tab-panel">
                <div className="tab-empty">
                    Loading issues...
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="tab-panel">
                <div className="tab-empty">
                    {error}
                </div>
            </div>
        );
    }

    return (
        <>
            <div className="tab-panel">
                <div className="tab-panel-header issues-header-row">
                    <div className="issue-filter-group">
                        <button
                            type="button"
                            className={`tab-panel-filter ${
                                filter === 'open'
                                    ? 'active'
                                    : ''
                            }`}
                            onClick={() => {
                                setFilter('open');
                            }}
                        >
                            <OpenIcon />
                            {openIssues.length} Open
                        </button>

                        <button
                            type="button"
                            className={`tab-panel-filter ${
                                filter === 'closed'
                                    ? 'active'
                                    : ''
                            }`}
                            onClick={() => {
                                setFilter('closed');
                            }}
                        >
                            <ClosedIcon />
                            {closedIssues.length} Closed
                        </button>
                    </div>

                    {canCreateIssues && (
                        <button
                            type="button"
                            className="issue-create-btn"
                            onClick={() => {
                                setSuccessMessage(null);
                                setIsCreateIssueOpen(true);
                            }}
                        >
                            New issue
                        </button>
                    )}
                </div>

                {successMessage && (
                    <div
                        className="tab-success-banner"
                        role="status"
                    >
                        {successMessage}
                    </div>
                )}

                <ul className="tab-item-list">
                    {shownIssues.map((issue) => {
                        const existingBounty =
                            bountyByIssueId.get(issue.id);

                        const canAddBounty =
                            canCreateBounties &&
                            !isClosed(issue) &&
                            !existingBounty;

                        return (
                            <li
                                key={issue.id}
                                className="tab-item"
                            >
                                {isClosed(issue) ? (
                                    <ClosedIcon />
                                ) : (
                                    <OpenIcon />
                                )}

                                <div className="tab-item-body">
                                    <div className="tab-item-title-row">
                                        <span className="tab-item-title">
                                            {issue.title}
                                        </span>

                                        {issue.status ===
                                            'IN_PROGRESS' && (
                                                <span className="issue-progress-badge">
                                                In progress
                                            </span>
                                            )}

                                        {existingBounty && (
                                            <span
                                                className={`issue-bounty-badge status-${existingBounty.status.toLowerCase()}`}
                                            >
                                                {existingBounty.amount.toLocaleString()}{' '}
                                                credits ·{' '}
                                                {
                                                    existingBounty.status
                                                }
                                            </span>
                                        )}
                                    </div>

                                    {issue.description && (
                                        <p className="issue-list-description">
                                            {issue.description}
                                        </p>
                                    )}

                                    <div className="tab-item-meta">
                                        #{issue.number}
                                        {' · '}
                                        opened{' '}
                                        {formatDate(
                                            issue.createdAt
                                        )}{' '}
                                        by{' '}
                                        <strong>
                                            {
                                                issue.authorUsername
                                            }
                                        </strong>
                                    </div>
                                </div>

                                {canAddBounty && (
                                    <div className="issue-bounty-actions">
                                        <button
                                            type="button"
                                            className="issue-add-bounty-btn"
                                            onClick={() => {
                                                setSuccessMessage(
                                                    null
                                                );

                                                setSelectedIssue(
                                                    issue
                                                );
                                            }}
                                        >
                                            Add bounty
                                        </button>
                                    </div>
                                )}
                            </li>
                        );
                    })}

                    {shownIssues.length === 0 && (
                        <li className="tab-empty">
                            No {filter} issues.
                        </li>
                    )}
                </ul>
            </div>

            <CreateIssueModal
                key={
                    isCreateIssueOpen ? 'open-issue-modal' : 'closed-issue-modal'
                }
                repositoryName={repoName}
                isOpen={isCreateIssueOpen}
                onClose={() => {
                    setIsCreateIssueOpen(false);
                }}
                onCreated={handleIssueCreated}
            />

            <CreateBountyModal
                key={selectedIssue?.id ?? 'closed-bounty-modal'}
                issue={selectedIssue}
                isOpen={selectedIssue !== null}
                onClose={() => {
                    setSelectedIssue(null);
                }}
                onCreated={handleBountyCreated}
            />
        </>
    );
}