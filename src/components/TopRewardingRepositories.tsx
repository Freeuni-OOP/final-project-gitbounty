import React from 'react';

interface TopRewardingRepositoriesProps {
  repositories: any[];
}

const TopRewardingRepositories: React.FC<TopRewardingRepositoriesProps> = ({
  repositories,
}) => {
  return (
    <div className="top-rewarding-repositories">
      {repositories.map((repository) => (
        <div key={repository.id} className="repository-info">
          <h3>{repository.repoName}</h3>
          <p>Total Reward: {repository.totalReward}</p>
        </div>
      ))}
    </div>
  );
};

export default TopRewardingRepositories;