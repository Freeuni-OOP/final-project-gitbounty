import React from 'react';
import '../styles/HomePage.css';

const HomePage: React.FC = () => {
  return (
    <div className="home-page">
      <div className="home-container">
        <h1>Welcome to GitBounty</h1>
        <p>Find and complete bounties on your favorite open-source projects</p>
        <div className="home-actions">
          <button className="btn btn-primary">Explore Bounties</button>
          <button className="btn btn-secondary">Post a Bounty</button>
        </div>
      </div>
    </div>
  );
};

export default HomePage;

