import React from 'react';

interface BountyCardProps {
  id: string;
  repoName: string;
  repoAvatar: string;
  issueTitle: string;
  rewardAmount: number;
  currency: 'USD' | 'Crypto';
}

const BountyCard: React.FC<BountyCardProps> = ({
  id,
  repoName,
  repoAvatar,
  issueTitle,
  rewardAmount,
  currency,
}) => {
  return (
    <div className="bounty-card">
      <img src={repoAvatar} alt={repoName} />
      <div className="bounty-info">
        <h3>{issueTitle}</h3>
        <p>
          {rewardAmount} {currency}
        </p>
      </div>
    </div>
  );
};

export default BountyCard;