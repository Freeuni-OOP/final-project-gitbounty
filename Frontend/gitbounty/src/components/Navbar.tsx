import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import '../styles/Navbar.css';
import {SignInButton} from "./buttons/auth/SignInButton.tsx";

const Navbar: React.FC = () => {
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="14" cy="14" r="13" fill="#F5A623" stroke="#E8960F" strokeWidth="1.5"/>
            <circle cx="14" cy="14" r="10" fill="#F7B731" stroke="#E8960F" strokeWidth="0.5"/>
            <text x="14" y="19" textAnchor="middle" fontSize="11" fontWeight="bold" fill="#7A4F00" fontFamily="serif">$</text>
          </svg>
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
            <Link
              to="/repositories"
              className={`nav-link ${location.pathname.startsWith('/repositories') ? 'active' : ''}`}
            >
              Repositories
            </Link>
          </li>
        </ul>

        <div className="nav-actions">
          <SignInButton/>
          <button className="nav-button-primary">Submit Bounty</button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

