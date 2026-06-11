import BountyCard from '../components/BountyCard';
import { useHomepageData } from '../hooks/useHomepageData';
import '../styles/HomePage.css';

const HomePage = () => {
  const {
    trendingBounties,
    personalizedBounties,
    trendingTopics,
    topRepositories,
    activityFeed,
    isLoading,
  } = useHomepageData();

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
        <p>Loading bounties...</p>
      </div>
    );
  }

  return (
    <div className="explore-page">
      <div className="hero-banner">
        <div className="hero-content">
          <h1 className="hero-title">
            Discover & Earn on <span className="hero-highlight">Open Source</span>
          </h1>
          <p className="hero-subtitle">
            Browse trending bounties, find issues matching your skills, and get rewarded for your contributions.
          </p>
          <div className="hero-actions">
            <button className="btn btn-primary">Explore Bounties</button>
            <button className="btn btn-secondary">Post a Bounty</button>
          </div>
        </div>
        <div className="hero-stats">
          <div className="hero-stat">
            <span className="hero-stat-value">$124K+</span>
            <span className="hero-stat-label">Total Bounties</span>
          </div>
          <div className="hero-stat">
            <span className="hero-stat-value">1,240</span>
            <span className="hero-stat-label">Open Issues</span>
          </div>
          <div className="hero-stat">
            <span className="hero-stat-value">340+</span>
            <span className="hero-stat-label">Contributors</span>
          </div>
        </div>
      </div>

      <div className="explore-layout">
        <main className="explore-main">
          <section className="feed-section">
            <div className="section-header">
              <h2 className="section-title">🔥 Trending Bounties</h2>
              <span className="section-subtitle">Highest reward, most active issues right now</span>
            </div>
            <div className="bounties-feed">
              {trendingBounties.map((bounty) => (
                <BountyCard key={bounty.id} {...bounty} />
              ))}
            </div>
          </section>

          <section className="feed-section">
            <div className="section-header">
              <h2 className="section-title">✨ Based on Your Interests</h2>
              <span className="section-subtitle">TypeScript, Rust, Python — your stack</span>
            </div>
            <div className="bounties-feed">
              {personalizedBounties.map((bounty) => (
                <BountyCard key={bounty.id} {...bounty} />
              ))}
            </div>
          </section>
        </main>

        <aside className="explore-sidebar">
          <div className="sidebar-card">
            <h3 className="sidebar-card-title">📌 Trending Topics</h3>
            <div className="topics-grid">
              {trendingTopics.map((topic) => (
                <button key={topic.tag} className="topic-pill">
                  {topic.tag}
                  <span className="topic-count">{topic.count}</span>
                </button>
              ))}
            </div>
          </div>

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
