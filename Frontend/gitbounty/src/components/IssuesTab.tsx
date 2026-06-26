import { useState } from 'react';
import { getIssues } from '../mocks/repositoriesMock';
import type { Issue } from '../mocks/repositoriesMock';
import '../styles/RepoTabs.css';

const LABEL_STYLE: Record<Issue['label'], { bg: string; color: string; border: string }> = {
  bug:           { bg: '#ffebe9', color: '#cf222e', border: '#ff818266' },
  enhancement:   { bg: '#ddf4ff', color: '#0969da', border: '#54aeff66' },
  documentation: { bg: '#dafbe1', color: '#1a7f37', border: '#4ac26b66' },
  question:      { bg: '#f5f0ff', color: '#8250df', border: '#d2a8ff66' },
};

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

export default function IssuesTab({ owner, repoName }: { owner: string; repoName: string }) {
  const [filter, setFilter] = useState<'open' | 'closed'>('open');
  const all = getIssues(owner, repoName);
  const shown = all.filter((i) => i.status === filter);
  const openCount = all.filter((i) => i.status === 'open').length;
  const closedCount = all.filter((i) => i.status === 'closed').length;

  return (
    <div className="tab-panel">
      <div className="tab-panel-header">
        <button
          className={`tab-panel-filter ${filter === 'open' ? 'active' : ''}`}
          onClick={() => setFilter('open')}
        >
          <OpenIcon /> {openCount} Open
        </button>
        <button
          className={`tab-panel-filter ${filter === 'closed' ? 'active' : ''}`}
          onClick={() => setFilter('closed')}
        >
          <ClosedIcon /> {closedCount} Closed
        </button>
      </div>

      <ul className="tab-item-list">
        {shown.map((issue) => {
          const ls = LABEL_STYLE[issue.label];
          return (
            <li key={issue.id} className="tab-item">
              {issue.status === 'open' ? <OpenIcon /> : <ClosedIcon />}
              <div className="tab-item-body">
                <div className="tab-item-title-row">
                  <span className="tab-item-title">{issue.title}</span>
                  <span
                    className="label-badge"
                    style={{ background: ls.bg, color: ls.color, borderColor: ls.border }}
                  >
                    {issue.label}
                  </span>
                </div>
                <div className="tab-item-meta">
                  #{issue.id} &middot; opened {issue.openedAt} by <strong>{issue.author}</strong>
                </div>
              </div>
            </li>
          );
        })}
        {shown.length === 0 && (
          <li className="tab-empty">No {filter} issues.</li>
        )}
      </ul>
    </div>
  );
}
