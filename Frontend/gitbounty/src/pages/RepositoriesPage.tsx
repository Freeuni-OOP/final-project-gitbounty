import { useRepositoriesData } from '../hooks/useRepositoriesData';
import { RepoCard } from '../components/RepoCard';
import '../styles/RepositoriesPage.css';

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
