import React from 'react';
import { mockUserProfile } from '../mocks/userProfileMock';

const UserProfilePage = () => {
  const userProfile = mockUserProfile;

  return (
    <div>
      <UserHeaderCard
        avatarUrl={userProfile.avatarUrl}
        githubUsername={userProfile.githubUsername}
        bio={userProfile.bio}
        joinedAt={userProfile.joinedAt}
        bountiesWon={userProfile.stats.bountiesWon}
        totalEarned={userProfile.stats.totalEarned}
      />
      <EarningSummary
        totalEarned={userProfile.stats.totalEarned}
        activeSubmissions={userProfile.stats.activeSubmissions}
      />
      <ActiveBountiesTab
        activeBounties={userProfile.activeBounties}
      />
      <SettingsLink />
    </div>
  );
};

const UserHeaderCard = ({
  avatarUrl,
  githubUsername,
  bio,
  joinedAt,
  bountiesWon,
  totalEarned,
}: {
  avatarUrl: string;
  githubUsername: string;
  bio: string;
  joinedAt: string;
  bountiesWon: number;
  totalEarned: number;
}) => {
  return (
    <div>
      <img src={avatarUrl} alt="Avatar" />
      <h2>{githubUsername}</h2>
      <p>{bio}</p>
      <p>Joined at: {joinedAt}</p>
      <p>Bounties Won: {bountiesWon}</p>
      <p>Total Earned: {totalEarned}</p>
    </div>
  );
};

const EarningSummary = ({
  totalEarned,
  activeSubmissions,
}: {
  totalEarned: number;
  activeSubmissions: number;
}) => {
  return (
    <div>
      <h2>Earning Summary</h2>
      <p>Total Earned: {totalEarned}</p>
      <p>Active Submissions: {activeSubmissions}</p>
    </div>
  );
};

const ActiveBountiesTab = ({
  activeBounties,
}: {
  activeBounties: any[];
}) => {
  return (
    <div>
      <h2>Active Bounties</h2>
      <ul>
        {activeBounties.map((bounty) => (
          <li key={bounty.id}>
            <h3>{bounty.title}</h3>
            <p>Reward: {bounty.reward}</p>
            <p>Status: {bounty.status}</p>
          </li>
        ))}
      </ul>
    </div>
  );
};

const SettingsLink = () => {
  return (
    <div>
      <button>Settings</button>
    </div>
  );
};

export default UserProfilePage;