import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useProfileData, parseCreatedAt } from '../hooks/useProfileData';
import { useRepositoriesData } from '../hooks/useRepositoriesData';
import { CreateRepositoryModal } from '../components/CreateRepositoryModal';
import { RepoCard } from '../components/RepoCard';
import type { ApiRepo } from '../hooks/useRepositoriesData';
import '../styles/ProfilePage.css';

const ProfilePage: React.FC = () => {
  const { user, isLoading, error, isUnauthenticated } = useProfileData();
  const { repos, isLoading: reposLoading, error: reposError } = useRepositoriesData();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();

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

  const handleRepoCreated = (repo: ApiRepo) => {
    navigate(`/repositories/${repo.ownerUsername}/${repo.name}`);
  };

  const myRepos = repos.filter((r) => r.ownerUsername === user.username);

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

      <div className="profile-repos-section">
        <div className="profile-repos-header">
          <h2 className="profile-repos-title">
            Repositories ({reposLoading ? '…' : myRepos.length})
          </h2>
          <button
            className="create-repo-btn"
            onClick={() => setIsModalOpen(true)}
          >
            + Create Repository
          </button>
        </div>

        {reposLoading ? (
          <p className="profile-repos-status">Loading repositories…</p>
        ) : reposError ? (
          <p className="profile-repos-status profile-repos-error">
            Failed to load repositories: {reposError}
          </p>
        ) : myRepos.length === 0 ? (
          <div className="profile-repos-empty">
            <p>You haven't created any repositories yet.</p>
          </div>
        ) : (
          <div className="profile-repos-list">
            {myRepos.map((repo) => (
              <RepoCard key={repo.id} repo={repo} />
            ))}
          </div>
        )}
      </div>

      <CreateRepositoryModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleRepoCreated}
        ownerName={user.username}
      />
    </div>
  );
};

export default ProfilePage;
