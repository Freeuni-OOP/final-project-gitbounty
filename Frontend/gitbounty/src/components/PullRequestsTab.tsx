import { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import '../styles/RepoTabs.css';

export type PullRequest = {
  id: number;
  title: string;
  author: string;
  openedAt: string;
  status: 'open' | 'merged' | 'closed';
  sourceBranch: string;
  targetBranch: string;
};

function OpenPRIcon() {
  return (
    <svg className="pr-status-icon open" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M1.5 3.25a2.25 2.25 0 1 1 3 2.122v5.256a2.251 2.251 0 1 1-1.5 0V5.372A2.25 2.25 0 0 1 1.5 3.25Zm5.677-.177L9.573.677A.25.25 0 0 1 10 .854V2.5h1A2.5 2.5 0 0 1 13.5 5v5.628a2.251 2.251 0 1 1-1.5 0V5a1 1 0 0 0-1-1h-1v1.646a.25.25 0 0 1-.427.177L7.177 3.427a.25.25 0 0 1 0-.354ZM3.75 2.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm0 9.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm8.25.75a.75.75 0 1 0 1.5 0 .75.75 0 0 0-1.5 0Z" />
    </svg>
  );
}

function MergedPRIcon() {
  return (
    <svg className="pr-status-icon merged" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M5.45 5.154A4.25 4.25 0 0 0 9.25 7.5h1.378a2.251 2.251 0 1 1 0 1.5H9.25A5.734 5.734 0 0 1 5 7.123v3.505a2.25 2.25 0 1 1-1.5 0V5.372a2.25 2.25 0 1 1 1.95-.218ZM4.25 13.5a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Zm8.5-4.5a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5ZM5 3.25a.75.75 0 1 0 0 .005V3.25Z" />
    </svg>
  );
}

function ClosedPRIcon() {
  return (
    <svg className="pr-status-icon closed" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M3.25 1A2.25 2.25 0 0 1 5 5.372v5.256a2.251 2.251 0 1 1-1.5 0V5.372A2.25 2.25 0 0 1 3.25 1Zm9.5 5.5a.75.75 0 0 1 .75.75v3.378a2.251 2.251 0 1 1-1.5 0V7.25a.75.75 0 0 1 .75-.75Zm-2.03-5.273a.75.75 0 0 1 1.06 0l.97.97.97-.97a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734l-.97.97.97.97a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215l-.97-.97-.97.97a.749.749 0 0 1-1.275-.326.749.749 0 0 1 .215-.734l.97-.97-.97-.97a.75.75 0 0 1 0-1.06ZM3.25 2.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm0 9.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm9.5 0a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Z" />
    </svg>
  );
}

function PRIcon({ status }: Readonly<{ status: PullRequest['status'] }>) {
  if (status === 'merged') return <MergedPRIcon />;
  if (status === 'closed') return <ClosedPRIcon />;
  return <OpenPRIcon />;
}

type Filter = 'open' | 'merged' | 'closed';

export default function PullRequestsTab({ repoName }: Readonly<{ repoName: string }>) {
  const [filter, setFilter] = useState<Filter>('open');
  const [pullRequests, setPullRequests] = useState<PullRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPullRequests = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await apiClient.get<PullRequest[]>(
          `/api/codebases/${repoName}/pull-requests`
        );
        setPullRequests(response.data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch pull requests');
        setPullRequests([]);
      } finally {
        setLoading(false);
      }
    };

    fetchPullRequests();
  }, [repoName]);

  const all = pullRequests;
  const shown = all.filter((pr) => pr.status === filter);
  const counts = { open: 0, merged: 0, closed: 0 } as Record<Filter, number>;
  all.forEach((pr) => counts[pr.status]++);

  const filters: Filter[] = ['open', 'merged', 'closed'];

  if (loading) {
    return (
      <div className="tab-panel">
        <div className="tab-panel-header">
          {filters.map((f) => (
            <button
              key={f}
              className={`tab-panel-filter ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
              disabled
            >
              <span className="pr-status-icon" /> 0 {f.charAt(0).toUpperCase() + f.slice(1)}
            </button>
          ))}
        </div>
        <ul className="tab-item-list">
          <li className="tab-empty">Loading pull requests...</li>
        </ul>
      </div>
    );
  }

  if (error) {
    return (
      <div className="tab-panel">
        <div className="tab-panel-header">
          {filters.map((f) => (
            <button
              key={f}
              className={`tab-panel-filter ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
              disabled
            >
              <span className="pr-status-icon" /> 0 {f.charAt(0).toUpperCase() + f.slice(1)}
            </button>
          ))}
        </div>
        <ul className="tab-item-list">
          <li className="tab-empty">Error: {error}</li>
        </ul>
      </div>
    );
  }

  return (
    <div className="tab-panel">
      <div className="tab-panel-header">
        {filters.map((f) => (
          <button
            key={f}
            className={`tab-panel-filter ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            <PRIcon status={f} /> {counts[f]} {f.charAt(0).toUpperCase() + f.slice(1)}
          </button>
        ))}
      </div>

      <ul className="tab-item-list">
        {shown.map((pr) => (
          <li key={pr.id} className="tab-item">
            <PRIcon status={pr.status} />
            <div className="tab-item-body">
              <div className="tab-item-title-row">
                <span className="tab-item-title">{pr.title}</span>
                <span className="branch-tag">{pr.sourceBranch} → {pr.targetBranch}</span>
              </div>
              <div className="tab-item-meta">
                #{pr.id} &middot; {pr.status} {pr.openedAt} by <strong>{pr.author}</strong>
              </div>
            </div>
          </li>
        ))}
        {shown.length === 0 && (
          <li className="tab-empty">No {filter} pull requests.</li>
        )}
      </ul>
    </div>
  );
}
