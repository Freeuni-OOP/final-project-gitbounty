import {type Filter, PRIcon} from "../../icons/pullrequest/PRIcons.tsx";

export type PullRequest = {
    id: number;
    title: string;
    author: string;
    openedAt: string;
    status: 'OPEN' | 'MERGED' | 'CLOSED';
    sourceBranch: string;
    targetBranch: string;
};
type PRListProps = {
    pullRequests: PullRequest[];
    filter: Filter;
    loading: boolean;
    error: string | null;
};

export default function PRList({ pullRequests, filter, loading, error }: Readonly<PRListProps>) {
    if (loading) {
        return (
            <ul className="tab-item-list">
                <li className="tab-empty">Loading pull requests...</li>
            </ul>
        );
    }

    if (error) {
        return (
            <ul className="tab-item-list">
                <li className="tab-empty error-text">Error: {error}</li>
            </ul>
        );
    }

    if (pullRequests.length === 0) {
        return (
            <ul className="tab-item-list">
                <li className="tab-empty">No {filter} pull requests.</li>
            </ul>
        );
    }

    return (
        <ul className="tab-item-list">
            {pullRequests.map((pr) => (
                <li key={pr.id} className="tab-item">
                    <PRIcon status={pr.status} />
                    <div className="tab-item-body">
                        <div className="tab-item-title-row">
                            <span className="tab-item-title">{pr.title}</span>
                            <span className="branch-tag">
                {pr.sourceBranch} → {pr.targetBranch}
              </span>
                        </div>
                        <div className="tab-item-meta">
                            #{pr.id} &middot; {pr.status} {pr.openedAt} by <strong>{pr.author}</strong>
                        </div>
                    </div>
                </li>
            ))}
        </ul>
    );
}