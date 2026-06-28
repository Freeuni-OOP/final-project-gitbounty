import { useEffect, useState } from 'react';
import apiClient from '../api/apiClient';
import '../styles/RepoTabs.css';

type IssueStatus = 'OPEN' | 'CLOSED' | string;

type ApiIssue = {
    id: number;
    title: string;
    description: string;
    status: IssueStatus;
    authorId: number;
    repositoryId: number;
    createdAt: string;
    updatedAt: string;
};

type Filter = 'open' | 'closed';

function OpenIcon() {
    return (
        <svg className="issue-status-icon open" viewBox="0 0 16 16" aria-hidden="true">
            <path d="M8 9.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z" />
            <path d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0ZM1.5 8a6.5 6.5 0 1 0 13 0 6.5 6.5 0 0 0-13 0Z" />
        </svg>
    );
}

function ClosedIcon() {
    return (
        <svg className="issue-status-icon closed" viewBox="0 0 16 16" aria-hidden="true">
            <path d="M11.28 6.78a.75.75 0 0 0-1.06-1.06L7.25 8.69 5.78 7.22a.75.75 0 0 0-1.06 1.06l2 2a.75.75 0 0 0 1.06 0l3.5-3.5Z" />
            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0Zm-1.5 0a6.5 6.5 0 1 0-13 0 6.5 6.5 0 0 0 13 0Z" />
        </svg>
    );
}

function isClosed(issue: ApiIssue) {
    return issue.status.toLowerCase() === 'closed';
}

function formatDate(date: string) {
    return new Date(date).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
    });
}

export default function IssuesTab({ repoName }: { owner: string; repoName: string }) {
    const [filter, setFilter] = useState<Filter>('open');
    const [issues, setIssues] = useState<ApiIssue[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function loadIssues() {
            try {
                setLoading(true);
                setError(null);

                const response = await apiClient.get<ApiIssue[]>(
                    `/api/codebases/${encodeURIComponent(repoName)}/issues`
                );

                if (!cancelled) {
                    setIssues(response.data);
                }
            } catch {
                if (!cancelled) {
                    setError('Failed to load issues.');
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadIssues();

        return () => {
            cancelled = true;
        };
    }, [repoName]);

    const openIssues = issues.filter((issue) => !isClosed(issue));
    const closedIssues = issues.filter(isClosed);
    const shown = filter === 'open' ? openIssues : closedIssues;

    if (loading) {
        return <div className="tab-panel"><div className="tab-empty">Loading issues...</div></div>;
    }

    if (error) {
        return <div className="tab-panel"><div className="tab-empty">{error}</div></div>;
    }

    return (
        <div className="tab-panel">
            <div className="tab-panel-header">
                <button
                    className={`tab-panel-filter ${filter === 'open' ? 'active' : ''}`}
                    onClick={() => setFilter('open')}
                >
                    <OpenIcon /> {openIssues.length} Open
                </button>
                <button
                    className={`tab-panel-filter ${filter === 'closed' ? 'active' : ''}`}
                    onClick={() => setFilter('closed')}
                >
                    <ClosedIcon /> {closedIssues.length} Closed
                </button>
            </div>

            <ul className="tab-item-list">
                {shown.map((issue) => (
                    <li key={issue.id} className="tab-item">
                        {isClosed(issue) ? <ClosedIcon /> : <OpenIcon />}
                        <div className="tab-item-body">
                            <div className="tab-item-title-row">
                                <span className="tab-item-title">{issue.title}</span>
                            </div>
                            <div className="tab-item-meta">
                                #{issue.id} &middot; opened {formatDate(issue.createdAt)} by{' '}
                                <strong>User #{issue.authorId}</strong>
                            </div>
                        </div>
                    </li>
                ))}
                {shown.length === 0 && (
                    <li className="tab-empty">No {filter} issues.</li>
                )}
            </ul>
        </div>
    );
}