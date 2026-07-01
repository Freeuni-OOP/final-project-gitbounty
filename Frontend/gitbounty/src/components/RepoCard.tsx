import { Link } from 'react-router-dom';
import type { ApiRepo } from '../hooks/useRepositoriesData';
import '../styles/RepositoriesPage.css';

export function RepoCard({ repo }: { repo: ApiRepo }) {
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
