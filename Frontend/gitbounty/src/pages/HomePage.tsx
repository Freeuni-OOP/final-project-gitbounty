import HeroBanner from '../components/HeroBanner';
import { useHomepageData } from '../hooks/useHomepageData';
import '../styles/HomePage.css';

const HomePage = () => {
  const { topRepositories, activityFeed, isLoading } = useHomepageData();

  const formatPool = (amount: number, currency: string) =>
    currency === 'ETH' ? `${amount} ETH` : `$${amount.toLocaleString()}`;

  const timeAgo = (dateString: string) => {
    const diff = Date.now() - new Date(dateString).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
  };

  if (isLoading) {
    return (
      <div className="explore-loading">
        <div className="loading-spinner" />
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div className="explore-page">
      <HeroBanner />
      <div className="explore-layout">
        <main className="explore-main">
          <div className="coming-soon">
            <h2>🚀 Bounties Coming Soon</h2>
            <p>Trending bounties and personalized feeds will appear here.</p>
          </div>
        </main>

        <aside className="explore-sidebar">
          <div className="sidebar-card">
            <h3 className="sidebar-card-title">🏆 Top Rewarding Repos</h3>
            <div className="repo-leaderboard">
              {topRepositories.map((repo, index) => (
                <div key={repo.name} className="repo-leaderboard-item">
                  <span className="repo-rank">#{index + 1}</span>
                  <img src={repo.avatarUrl} alt={repo.name} className="repo-mini-avatar" />
                  <div className="repo-leaderboard-info">
                    <span className="repo-leaderboard-name">{repo.name}</span>
                    <span className="repo-leaderboard-bounties">{repo.openBounties} open bounties</span>
                  </div>
                  <span className="repo-pool">{formatPool(repo.totalBountyPool, repo.currency)}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="sidebar-card">
            <h3 className="sidebar-card-title">⚡ Live Activity</h3>
            <div className="activity-feed">
              {activityFeed.map((item) => (
                <div key={item.id} className="activity-item">
                  <div className="activity-dot" />
                  <div className="activity-text">
                    <span className="activity-username">@{item.username}</span>{' '}
                    {item.action} a{' '}
                    <span className="activity-reward">
                      {item.currency === 'ETH' ? `${item.amount} ETH` : `$${item.amount}`}
                    </span>{' '}
                    bounty on <span className="activity-repo">{item.repoName}</span>
                  </div>
                  <span className="activity-time">{timeAgo(item.timestamp)}</span>
                </div>
              ))}
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default HomePage;
