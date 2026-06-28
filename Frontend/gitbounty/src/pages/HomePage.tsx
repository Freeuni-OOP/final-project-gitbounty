import { Link } from 'react-router-dom';
import { useHomepageData } from '../hooks/useHomepageData';
import type { BountyDTO } from '../hooks/useHomepageData';
import '../styles/HomePage.css';

const STATUS_LABEL: Record<BountyDTO['status'], string> = {
  OPEN: 'Open',
  ASSIGNED: 'Assigned',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

const TEAM_MEMBERS = [
  { name: 'Gigi Chachua',         initials: 'GC', color: '#6366f1' },
  { name: 'Giorgi Chokoshvili',   initials: 'GC', color: '#ec4899' },
  { name: 'Mishiko Kurua',        initials: 'MK', color: '#14b8a6' },
  { name: 'Nikoloz Lomsadze',     initials: 'NL', color: '#f59e0b' },
  { name: 'Ekaterine Sheshelidze',initials: 'ES', color: '#10b981' },
];

const HomePage = () => {
  const { bounties, isLoading, error } = useHomepageData();
  const recentBounties = bounties.slice(0, 4);
  const showBounties = !isLoading && !error && recentBounties.length > 0;

  return (
    <div className="home-page">

      {/* 1. HERO */}
      <div className="hero-banner">
        <div className="hero-content">
          <h1 className="hero-title">
            Discover & Earn on <span className="hero-highlight">Open Source</span>
          </h1>
          <p className="hero-subtitle">
            Browse trending bounties, find issues matching your skills, and get rewarded for your contributions.
          </p>
          <div className="hero-actions">
            <Link to="/bounties" className="btn btn-primary">Explore Bounties</Link>
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

      {/* 2. HOW IT WORKS */}
      <section className="lp-section">
        <h2 className="lp-section-title">How It Works</h2>
        <div className="steps-row">
          <div className="step-card">
            <div className="step-number">1</div>
            <h3 className="step-title">Find an Issue</h3>
            <p className="step-desc">Browse open issues with bounties attached</p>
          </div>
          <div className="step-connector" />
          <div className="step-card">
            <div className="step-number">2</div>
            <h3 className="step-title">Submit a Fix</h3>
            <p className="step-desc">Open a PR and claim the bounty</p>
          </div>
          <div className="step-connector" />
          <div className="step-card">
            <div className="step-number">3</div>
            <h3 className="step-title">Get Paid</h3>
            <p className="step-desc">Bounty released from escrow on merge</p>
          </div>
        </div>
      </section>

      {/* 3. WHY GITBOUNTY */}
      <div className="lp-alt-bg">
        <section className="lp-section">
          <h2 className="lp-section-title">Why GitBounty</h2>
          <div className="features-row">
            <div className="feature-card">
              <h3 className="feature-title">Open Source First</h3>
              <p className="feature-desc">Built for the open source community</p>
            </div>
            <div className="feature-card">
              <h3 className="feature-title">Escrow Protected</h3>
              <p className="feature-desc">Funds held securely until work is verified</p>
            </div>
            <div className="feature-card">
              <h3 className="feature-title">Transparent Reviews</h3>
              <p className="feature-desc">Everything tracked via pull requests</p>
            </div>
          </div>
        </section>
      </div>

      {/* 4. RECENT BOUNTIES */}
      {showBounties && (
        <section className="lp-section">
          <h2 className="lp-section-title">Recent Bounties</h2>
          <div className="recent-bounties-grid">
            {recentBounties.map((bounty) => (
              <div key={bounty.id} className="bounty-card">
                <div className="bounty-card-header">
                  <span className={`bounty-status bounty-status--${bounty.status.toLowerCase()}`}>
                    {STATUS_LABEL[bounty.status]}
                  </span>
                </div>
                <h3 className="bounty-issue-title">{bounty.title}</h3>
                <div className="bounty-card-footer">
                  <span className="bounty-reward-badge">${bounty.amount.toLocaleString()}</span>
                </div>
              </div>
            ))}
          </div>
          <div className="see-all-link">
            <Link to="/bounties">See all bounties →</Link>
          </div>
        </section>
      )}

      {/* 5. MEET THE TEAM */}
      <div className="lp-dark-bg">
        <section className="lp-section">
          <h2 className="lp-section-title lp-section-title--light">Meet the Team</h2>
          <div className="team-row">
            {TEAM_MEMBERS.map((member) => (
              <div key={member.name} className="team-member">
                <div className="team-avatar" style={{ backgroundColor: member.color }}>
                  {member.initials}
                </div>
                <p className="team-name">{member.name}</p>
                <p className="team-role">Software Engineer</p>
              </div>
            ))}
          </div>
        </section>
      </div>

    </div>
  );
};

export default HomePage;
