import React from 'react';
import { useProfileData, parseCreatedAt } from '../hooks/useProfileData';
import '../styles/ProfilePage.css';

const ProfilePage: React.FC = () => {
  const { user, isLoading, error, isUnauthenticated } = useProfileData();

  if (isLoading) {
    return (
      <div className="profile-loading">
        <div className="loading-spinner" />
        <p>Loading profile...</p>
      </div>
    );
  }

  if (isUnauthenticated) {
    return (
      <div className="profile-loading">
        <p>Please log in to view your profile.</p>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="profile-loading">
        <p>Failed to load profile{error ? `: ${error}` : ''}.</p>
      </div>
    );
  }

  const joinedDate = parseCreatedAt(user.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  const initials = user.username.slice(0, 2).toUpperCase();

  return (
    <div className="profile-container">
      <div className="user-header-card">
        <div className="header-content">
          <div className="avatar-section">
            <div className="avatar-initials">{initials}</div>
          </div>
          <div className="user-info">
            <div className="username-row">
              <h1 className="username">{user.username}</h1>
            </div>
            <p className="bio">{user.email}</p>
            <p className="joined-date">Member since {joinedDate}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
