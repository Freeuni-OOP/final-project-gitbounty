import '../styles/HomePage.css';

const HeroBanner = () => (
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
);

export default HeroBanner;
