import { Link } from 'react-router-dom';
import { mockRepositories } from '../mocks/repositoriesMock';
import type { MockRepo } from '../mocks/repositoriesMock';
import '../styles/RepositoriesPage.css';

function fmtNum(n: number): string {
  return n >= 1000 ? `${(n / 1000).toFixed(1)}k` : String(n);
}

function RepoCard({ repo }: { repo: MockRepo }) {
  return (
    <Link
      to={`/repositories/${repo.owner}/${repo.name}`}
      className="repo-card"
    >
      <div className="repo-card-header">
        <span className="repo-card-name">
          <span className="repo-card-owner">{repo.owner}</span>
          <span className="repo-card-slash">/</span>
          <span className="repo-card-repo">{repo.name}</span>
        </span>
        <span className="repo-card-public-badge">Public</span>
      </div>

      {repo.description && (
        <p className="repo-card-desc">{repo.description}</p>
      )}

      <div className="repo-card-meta">
        <span
          className="repo-card-lang"
          style={{ '--lang-color': repo.languageColor } as React.CSSProperties}
        >
          <span className="lang-dot" />
          {repo.language}
        </span>
        <span className="repo-card-stat">
          <svg className="stat-icon" viewBox="0 0 16 16" aria-hidden="true">
            <path d="M8 .25a.75.75 0 0 1 .673.418l1.882 3.815 4.21.612a.75.75 0 0 1 .416 1.279l-3.046 2.97.719 4.192a.751.751 0 0 1-1.088.791L8 12.347l-3.766 1.98a.75.75 0 0 1-1.088-.79l.72-4.194L.818 6.374a.75.75 0 0 1 .416-1.28l4.21-.611L7.327.668A.75.75 0 0 1 8 .25Z" />
          </svg>
          {fmtNum(repo.stars)}
        </span>
        <span className="repo-card-stat">
          <svg className="stat-icon" viewBox="0 0 16 16" aria-hidden="true">
            <path d="M5 5.372v.878c0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75v-.878a2.25 2.25 0 1 1 1.5 0v.878a2.25 2.25 0 0 1-2.25 2.25h-1.5v2.128a2.251 2.251 0 1 1-1.5 0V8.5h-1.5A2.25 2.25 0 0 1 3.5 6.25v-.878a2.25 2.25 0 1 1 1.5 0ZM5 3.25a.75.75 0 1 0-1.5 0 .75.75 0 0 0 1.5 0Zm6.75.75a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Zm-3 8.75a.75.75 0 1 0-1.5 0 .75.75 0 0 0 1.5 0Z" />
          </svg>
          {fmtNum(repo.forks)}
        </span>
        <span className="repo-card-updated">Updated {repo.lastUpdated}</span>
      </div>
    </Link>
  );
}

export default function RepositoriesPage() {
  return (
    <div className="repositories-page">
      <div className="repositories-header">
        <h1 className="repositories-title">Repositories</h1>
        <p className="repositories-subtitle">{mockRepositories.length} repositories</p>
      </div>
      <div className="repositories-list">
        {mockRepositories.map((repo) => (
          <RepoCard key={`${repo.owner}/${repo.name}`} repo={repo} />
        ))}
      </div>
    </div>
  );
}
