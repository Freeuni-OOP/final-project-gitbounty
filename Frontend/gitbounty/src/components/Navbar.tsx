import React, { useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import '../styles/Navbar.css';
import {SignInButton} from "./buttons/auth/SignInButton.tsx";
import { useAuth } from '../auth/useAuth';
import { useBalance } from '../context/BalanceContext';

const Navbar: React.FC = () => {
  const location = useLocation();
  const { isLoading: isAuthLoading, authenticated } = useAuth();
  const { balance, refreshBalance } = useBalance();

  const isActive = (path: string) => location.pathname === path;

  // Custom Formatter Rule (e.g., 5000 -> 5k, 1000000+ -> 999k+)
  const formatBalance = (num: number): string => {
    if (num >= 1000000) {
      return '999k+';
    }
    if (num >= 1000) {
      const thousands = num / 1000;
      // If it's a clean whole number (like 5k), remove trailing decimal zero
      return thousands % 1 === 0 ? `${thousands}k` : `${thousands.toFixed(1)}k`;
    }
    return `${num}`;
  };

  useEffect(() => {
    // Don't fetch until auth is confirmed ready.
    if (isAuthLoading || !authenticated) return;

    refreshBalance();
  }, [isAuthLoading, authenticated, refreshBalance]);

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
          <li className="nav-item">
            <Link
                to="/buy-credits"
                className={`nav-link ${isActive('/buy-credits') ? 'active' : ''}`}
            >
              Buy Credits
            </Link>
          </li>
        </ul>

        <div className="nav-actions" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {/* Wallet Element */}
          <div
              title={`Exact Balance: ${balance} Credits`} // Hovering reveals exact unformatted integer
              style={{
                display: 'flex',
                alignItems: 'center',
                backgroundColor: '#0B132B',
                padding: '6px 14px',
                borderRadius: '20px',
                border: '1px solid #5BC0BE',
                fontSize: '13px',
                fontWeight: 'bold',
                color: '#5BC0BE',
                userSelect: 'none',
                cursor: 'help'
              }}
          >
            <span style={{ color: '#CDD6F4', marginRight: '6px', fontWeight: 'normal' }}>Wallet:</span>
            {formatBalance(balance)} cr
          </div>
          <SignInButton/>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

