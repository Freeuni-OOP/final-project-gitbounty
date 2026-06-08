export interface BountyCardProps {
  id: string;
  repoName: string;
  repoAvatar: string;
  issueTitle: string;
  rewardAmount: number;
  currency: 'USD' | 'Crypto';
}