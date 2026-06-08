export interface UserProfile {
  id: string;
  githubUsername: string;
  avatarUrl: string;
  bio: string;
  joinedAt: string;
  stats: {
    bountiesWon: number;
    totalEarned: number;
    activeSubmissions: number;
  };
  activeBounties: Array<{
    id: string;
    title: string;
    reward: string;
    status: 'open' | 'in_review' | 'completed';
  }>;
}

export const mockUserProfile: UserProfile = {
  id: '12345',
  githubUsername: 'johnDoe',
  avatarUrl: 'https://via.placeholder.com/150',
  bio: 'Software Engineer and Open Source Enthusiast',
  joinedAt: '2022-01-01',
  stats: {
    bountiesWon: 10,
    totalEarned: 1000,
    activeSubmissions: 5,
  },
  activeBounties: [
    {
      id: 'bounty-1',
      title: 'Fix Bug in GitBounty',
      reward: '100 USD',
      status: 'open',
    },
    {
      id: 'bounty-2',
      title: 'Implement New Feature',
      reward: '500 USD',
      status: 'in_review',
    },
    {
      id: 'bounty-3',
      title: 'Optimize Performance',
      reward: '200 USD',
      status: 'completed',
    },
  ],
};