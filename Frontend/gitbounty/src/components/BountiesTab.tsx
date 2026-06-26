import { useState } from 'react';
import { getBounties } from '../mocks/repositoriesMock';
import type { RepoBounty } from '../mocks/repositoriesMock';
import '../styles/RepoTabs.css';

const STATUS_STYLE: Record<RepoBounty['status'], { bg: string; color: string }> = {
  open:      { bg: '#dafbe1', color: '#1a7f37' },
  claimed:   { bg: '#fff8c5', color: '#9a6700' },
  completed: { bg: '#ddf4ff', color: '#0969da' },
};

type Filter = 'all' | RepoBounty['status'];
const FILTERS: Filter[] = ['all', 'open', 'claimed', 'completed'];

export default function BountiesTab({ owner, repoName }: { owner: string; repoName: string }) {
  const [filter, setFilter] = useState<Filter>('all');
  const all = getBounties(owner, repoName);
  const shown = filter === 'all' ? all : all.filter((b) => b.status === filter);
  const total = all.reduce((sum, b) => sum + b.amount, 0);

  return (
    <div className="tab-panel">
      <div className="tab-panel-header bounties-header-row">
        <div className="bounty-filters">
          {FILTERS.map((f) => (
            <button
              key={f}
              className={`tab-panel-filter ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
            >
              {f === 'all' ? `All (${all.length})` : `${f.charAt(0).toUpperCase() + f.slice(1)} (${all.filter((b) => b.status === f).length})`}
            </button>
          ))}
        </div>
        <span className="bounty-total-label">${total.toLocaleString()} total posted</span>
      </div>

      <ul className="tab-item-list">
        {shown.map((bounty) => {
          const ss = STATUS_STYLE[bounty.status];
          return (
            <li key={bounty.id} className="tab-item bounty-item">
              <div className="bounty-amount">${bounty.amount}</div>
              <div className="tab-item-body">
                <div className="tab-item-title-row">
                  <span className="tab-item-title">
                    <span className="bounty-issue-ref">#{bounty.issueId}</span>{' '}
                    {bounty.issueTitle}
                  </span>
                  <span
                    className="bounty-status-badge"
                    style={{ background: ss.bg, color: ss.color }}
                  >
                    {bounty.status}
                  </span>
                </div>
                <div className="tab-item-meta">
                  Posted by <strong>{bounty.poster}</strong>
                </div>
              </div>
            </li>
          );
        })}
        {shown.length === 0 && (
          <li className="tab-empty">No {filter === 'all' ? '' : filter} bounties.</li>
        )}
      </ul>
    </div>
  );
}
