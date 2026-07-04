import {type Filter, PRIcon, type PullRequest} from "../../icons/pullrequest/PRIcons.tsx";
import { Link, useParams } from 'react-router-dom';

type PRListProps = {
    pullRequests: PullRequest[];
    filter: Filter;
    loading: boolean;
    error: string | null;
};

export default function PRList({ pullRequests, filter, loading, error }: Readonly<PRListProps>) {
    const { owner = '', repoName = '' } = useParams<{ owner: string; repoName: string }>();

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
                    <Link
                        to={`/repositories/${owner}/${repoName}/pull-requests/${pr.number}`}
                        className="tab-item-link"
                    >
                        <PRIcon status={pr.status} />
                        <div className="tab-item-body">
                            <div className="tab-item-title-row">
                                <span className="tab-item-title">{pr.title}</span>
                                <span className="branch-tag">
                        {pr.sourceBranch} → {pr.targetBranch}
                      </span>
                            </div>
                            <div className="tab-item-meta">
                                #{pr.number} &middot; {pr.status} {new Date(pr.createdAt).toLocaleDateString()} by <strong>{pr.authorUsername}</strong>
                            </div>
                        </div>
                    </Link>
                </li>
            ))}
        </ul>
    );
}