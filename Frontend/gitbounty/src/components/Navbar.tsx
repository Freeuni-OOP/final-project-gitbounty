import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import '../styles/Navbar.css';

const Navbar: React.FC = () => {
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo">
          <span className="logo-icon">🎯</span>
          GitBounty
        </Link>

        <ul className="nav-menu">
          <li className="nav-item">
            <Link
              to="/"
              className={`nav-link ${isActive('/') ? 'active' : ''}`}
            >
              Home
            </Link>
          </li>
          <li className="nav-item">
            <Link
              to="/profile"
              className={`nav-link ${isActive('/profile') ? 'active' : ''}`}
            >
              Profile
            </Link>
          </li>
          <li className="nav-item">
            <Link
              to="/bounties"
              className={`nav-link ${isActive('/bounties') ? 'active' : ''}`}
            >
              Bounties
            </Link>
          </li>
          <li className="nav-item">
            <a href="#about" className="nav-link">
              About
            </a>
          </li>
        </ul>

        <div className="nav-actions">
          <button className="nav-button-secondary">Sign In</button>
          <button className="nav-button-primary">Submit Bounty</button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

