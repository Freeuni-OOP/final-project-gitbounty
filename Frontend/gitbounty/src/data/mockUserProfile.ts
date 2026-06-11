import type {UserProfile} from '../types/UserProfile';

export const mockUserProfile: UserProfile = {
  id: 'user-123',
  githubUsername: 'developername',
  avatarUrl: 'https://avatars.githubusercontent.com/u/12345678?v=4',
  bio: 'Full-stack developer passionate about open-source and tackling complex bugs 🚀',
  joinedAt: '2024-01-15',
  stats: {
    bountiesWon: 12,
    totalEarned: 2450,
    activeSubmissions: 3,
  },
  activeBounties: [
    {
      id: 'b1',
      title: 'Fix critical memory leak in user service',
      reward: '$500',
      status: 'in_review',
    },
    {
      id: 'b2',
      title: 'Implement dark mode toggle feature',
      reward: '$250',
      status: 'open',
    },
    {
      id: 'b3',
      title: 'Optimize API response time',
      reward: '$800',
      status: 'open',
    },
  ],
};

