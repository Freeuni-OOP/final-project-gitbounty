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

