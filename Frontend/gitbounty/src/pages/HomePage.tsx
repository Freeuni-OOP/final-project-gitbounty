import HeroBanner from '../components/HeroBanner';
import { useHomepageData } from '../hooks/useHomepageData';
import type { BountyDTO } from '../hooks/useHomepageData';
import '../styles/HomePage.css';

const STATUS_LABEL: Record<BountyDTO['status'], string> = {
  OPEN: 'Open',
  ASSIGNED: 'Assigned',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

const HomePage = () => {
  const { bounties, isLoading, error } = useHomepageData();

  if (isLoading) {
    return (
      <div className="explore-loading">
        <div className="loading-spinner" />
        <p>Loading...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="explore-loading">
        <p>Failed to load bounties: {error}</p>
      </div>
    );
  }

  return (
    <div className="explore-page">
      <HeroBanner />
      <div className="feed-layout">
        <div className="feed-section">
          <div className="section-header">
            <h2 className="section-title">Bounties</h2>
            <span className="section-subtitle">{bounties.length} bounty{bounties.length !== 1 ? 's' : ''} available</span>
          </div>
          {bounties.length === 0 ? (
            <p className="bounties-empty">No bounties found.</p>
          ) : (
            <div className="bounties-feed">
              {bounties.map((bounty) => (
                <div key={bounty.id} className="bounty-card">
                  <div className="bounty-card-header">
                    <span className={`bounty-status bounty-status--${bounty.status.toLowerCase()}`}>
                      {STATUS_LABEL[bounty.status]}
                    </span>
                    <span className="bounty-time">Issue #{bounty.issueId}</span>
                  </div>
                  <h3 className="bounty-issue-title">{bounty.title}</h3>
                  {bounty.description && (
                    <p className="bounty-description">{bounty.description}</p>
                  )}
                  <div className="bounty-card-footer">
                    <span className="bounty-reward-badge">${bounty.amount.toLocaleString()}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default HomePage;
