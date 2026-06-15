import React, { useState } from 'react';
import type {UserProfile} from '../types/UserProfile';
import { mockUserProfile } from '../data/mockUserProfile';
import '../styles/ProfilePage.css';

const ProfilePage: React.FC = () => {
  const [userProfile] = useState<UserProfile>(mockUserProfile);
  const [activeTab, setActiveTab] = useState<'active' | 'history'>('active');

  const handleSettingsClick = () => {
    console.log('Navigate to settings page');
    // TODO: Route to profile settings page
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const getStatusColor = (status: 'open' | 'in_review' | 'completed') => {
    switch (status) {
      case 'open':
        return '#10b981'; // green
      case 'in_review':
        return '#f59e0b'; // amber
      case 'completed':
        return '#3b82f6'; // blue
      default:
        return '#6b7280'; // gray
    }
  };

  return (
    <div className="profile-container">
      {/* User Header Card */}
      <div className="user-header-card">
        <div className="header-content">
          <div className="avatar-section">
            <img
              src={userProfile.avatarUrl}
              alt={userProfile.githubUsername}
              className="avatar-image"
            />
          </div>
          <div className="user-info">
            <div className="username-row">
              <h1 className="username">{userProfile.githubUsername}</h1>
              <button
                className="settings-button"
                onClick={handleSettingsClick}
                title="Profile Settings"
              >
                ⚙️
              </button>
            </div>
            <p className="bio">{userProfile.bio}</p>
            <p className="joined-date">
              Joined {formatDate(userProfile.joinedAt)}
            </p>
          </div>
        </div>
      </div>

      {/* Stats Summary Section */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">Bounties Won</div>
          <div className="stat-value">{userProfile.stats.bountiesWon}</div>
          <div className="stat-subtext">Total completed</div>
        </div>

        <div className="stat-card">
          <div className="stat-label">Total Earned</div>
          <div className="stat-value">${userProfile.stats.totalEarned}</div>
          <div className="stat-subtext">USD</div>
        </div>

        <div className="stat-card">
          <div className="stat-label">Active Submissions</div>
          <div className="stat-value">{userProfile.stats.activeSubmissions}</div>
          <div className="stat-subtext">In progress</div>
        </div>
      </div>

      {/* Earning Summary Section */}
      <div className="earning-summary">
        <h2 className="section-title">💰 Earning Summary</h2>
        <div className="earning-cards">
          <div className="earning-card">
            <div className="earning-card-label">Locked in Active Bounties</div>
            <div className="earning-card-value">
              ${userProfile.activeBounties.reduce((sum, bounty) => {
                const reward = parseInt(bounty.reward.replace('$', ''), 10);
                return sum + reward;
              }, 0)}
            </div>
            <div className="earning-card-subtext">Potential earnings</div>
          </div>

          <div className="earning-card">
            <div className="earning-card-label">Lifetime Earnings</div>
            <div className="earning-card-value">${userProfile.stats.totalEarned}</div>
            <div className="earning-card-subtext">Withdrawn</div>
          </div>
        </div>
      </div>

      {/* Active Bounties Section */}
      <div className="bounties-section">
        <div className="bounties-header">
          <h2 className="section-title">📋 My Bounties</h2>
          <div className="tab-buttons">
            <button
              className={`tab-button ${activeTab === 'active' ? 'active' : ''}`}
              onClick={() => setActiveTab('active')}
            >
              Active
            </button>
            <button
              className={`tab-button ${activeTab === 'history' ? 'active' : ''}`}
              onClick={() => setActiveTab('history')}
            >
              History
            </button>
          </div>
        </div>

        {activeTab === 'active' && (
          <div className="bounties-list">
            {userProfile.activeBounties.length > 0 ? (
              userProfile.activeBounties.map((bounty) => (
                <div key={bounty.id} className="bounty-card">
                  <div className="bounty-left">
                    <h3 className="bounty-title">{bounty.title}</h3>
                    <div className="bounty-meta">
                      <span className="bounty-id">ID: {bounty.id}</span>
                    </div>
                  </div>
                  <div className="bounty-right">
                    <div className="bounty-reward">{bounty.reward}</div>
                    <div
                      className="bounty-status"
                      style={{ borderLeftColor: getStatusColor(bounty.status) }}
                    >
                      {bounty.status.replace('_', ' ')}
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="empty-state">
                <p>No active bounties yet. Start exploring available bounties!</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bounties-list">
            <div className="empty-state">
              <p>Bounty history coming soon...</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProfilePage;

