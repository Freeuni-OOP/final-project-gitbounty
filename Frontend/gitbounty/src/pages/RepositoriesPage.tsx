import { Link } from 'react-router-dom';
import { useRepositoriesData } from '../hooks/useRepositoriesData';
import type { ApiRepo } from '../hooks/useRepositoriesData';
import '../styles/RepositoriesPage.css';

function RepoCard({ repo }: { repo: ApiRepo }) {
  return (
    <Link
      to={`/repositories/${repo.ownerUsername}/${repo.name}`}
      className="repo-card"
    >
      <div className="repo-card-header">
        <span className="repo-card-name">
          <span className="repo-card-owner">{repo.ownerUsername}</span>
          <span className="repo-card-slash">/</span>
          <span className="repo-card-repo">{repo.name}</span>
        </span>
        <span className="repo-card-public-badge">Public</span>
      </div>

      {repo.description && (
        <p className="repo-card-desc">{repo.description}</p>
      )}

      <div className="repo-card-meta">
        <span className="repo-card-updated">
          Created {new Date(repo.createdAt).toLocaleDateString()}
        </span>
      </div>
    </Link>
  );
}

export default function RepositoriesPage() {
  const { repos, isLoading, error } = useRepositoriesData();

  if (isLoading) {
    return (
      <div className="repositories-page">
        <p className="repositories-loading">Loading repositories…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="repositories-page">
        <p className="repositories-error">Failed to load repositories: {error}</p>
      </div>
    );
  }

  return (
    <div className="repositories-page">
      <div className="repositories-header">
        <h1 className="repositories-title">Repositories</h1>
        <p className="repositories-subtitle">{repos.length} repositories</p>
      </div>
      <div className="repositories-list">
        {repos.map((repo) => (
          <RepoCard key={repo.id} repo={repo} />
        ))}
      </div>
    </div>
  );
}
