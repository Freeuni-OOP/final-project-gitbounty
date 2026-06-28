import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import '../styles/Navbar.css';
import { SignInButton } from "./buttons/auth/SignInButton.tsx";
import { paymentService } from '../api/paymentService';
import { useAuth } from '../auth/useAuth';

const Navbar: React.FC = () => {
  const location = useLocation();
  const [balance, setBalance] = useState<number>(0);
  const { isLoading: isAuthLoading, authenticated } = useAuth();

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

  const fetchBalance = () => {
    paymentService.getMyBalance()
        .then((res: any) => {
          // Safe unpacking: checks if Axios returned raw response or nested response object
          const data = res?.data && res?.status ? res.data : res;
          setBalance(data?.creditBalance ?? 0);
        })
        .catch((err: any) => console.error("Could not fetch navbar balance", err));
  };


  useEffect(() => {
    // Don't fetch until auth is confirmed ready. Replaces the old 400ms retry guess.
    if (isAuthLoading || !authenticated) return;

    fetchBalance();
  }, [isAuthLoading, authenticated]);

  useEffect(() => {
    // Listen for broadcast pings when purchases are submitted successfully.
    // Kept separate from the auth-gated effect above since this listener
    // should stay registered regardless of auth state changes.
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
              <Link to="/" className={`nav-link ${isActive('/') ? 'active' : ''}`}>
                Home
              </Link>
            </li>
            <li className="nav-item">
              <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
                Profile
              </Link>
            </li>
            <li className="nav-item">
              <Link to="/bounties" className={`nav-link ${isActive('/bounties') ? 'active' : ''}`}>
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
              <Link to="/buy-credits" className={`nav-link ${isActive('/buy-credits') ? 'active' : ''}`}>
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

            {/* Wallet Element with formatting rules active */}
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
            <button className="nav-button-primary">Submit Bounty</button>
          </div>
        </div>
      </nav>
  );
};

export default Navbar;