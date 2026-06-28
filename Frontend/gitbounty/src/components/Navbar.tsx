import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import '../styles/Navbar.css';
import { SignInButton } from "./buttons/auth/SignInButton.tsx";
import { paymentService } from '../api/paymentService';

const Navbar: React.FC = () => {
  const location = useLocation();
  const [balance, setBalance] = useState<number>(0);

  const isActive = (path: string) => location.pathname === path;

  // Function to pull the latest user balance
  const fetchBalance = () => {
    paymentService.getMyBalance()
        .then((data: any) => setBalance(data.creditBalance))
        .catch((err: any) => console.error("Could not fetch navbar balance", err));
  };

  useEffect(() => {
    // Fetch user balance on initial navbar mount
    fetchBalance();

    // Set up listener to automatically catch updates from BuyCreditsPage
    const handleBalanceUpdate = () => {
      fetchBalance();
    };

    window.addEventListener('balanceUpdated', handleBalanceUpdate);

    return () => {
      window.removeEventListener('balanceUpdated', handleBalanceUpdate);
    };
  }, []);

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
              <Link
                  to="/repositories"
                  className={`nav-link ${location.pathname.startsWith('/repositories') ? 'active' : ''}`}
              >
                Repositories
              </Link>
            </li>
            {/* Buy Credits link using existing rules */}
            <li className="nav-item">
              <Link
                  to="/buy-credits"
                  className={`nav-link ${isActive('/buy-credits') ? 'active' : ''}`}
              >
                Buy Credits
              </Link>
            </li>
            <li className="nav-item">
              <a href="#about" className="nav-link">
                About
              </a>
            </li>
          </ul>

          <div className="nav-actions" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>

            {/* Wallet Balance Display Pill */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              backgroundColor: '#0B132B',
              padding: '6px 14px',
              borderRadius: '20px',
              border: '1px solid #5BC0BE',
              fontSize: '13px',
              fontWeight: 'bold',
              color: '#5BC0BE',
              userSelect: 'none'
            }}>
              <span style={{ color: '#CDD6F4', marginRight: '6px', fontWeight: 'normal' }}>Wallet:</span>
              {balance} cr
            </div>

            <SignInButton/>
            <button className="nav-button-primary">Submit Bounty</button>
          </div>
        </div>
      </nav>
  );
};

export default Navbar;