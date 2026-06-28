export interface BountyCardProps {
    id: string;
    repoName: string;
    repoAvatar: string;
    issueTitle: string;
    rewardAmount: number;
    currency: 'USD' | 'USDC' | 'ETH';
    difficulty: 'Good First Issue' | 'Medium' | 'Advanced';
    languages: string[];
    createdAt: string;
}

export interface TrendingTopic {
    tag: string;
    count: number;
}

export interface TopRepository {
    name: string;
    avatarUrl: string;
    totalBountyPool: number;
    currency: 'USD' | 'USDC' | 'ETH';
    openBounties: number;
}

export interface ActivityItem {
    id: string;
    username: string;
    action: string;
    amount: number;
    currency: 'USD' | 'USDC' | 'ETH';
    repoName: string;
    timestamp: string;
}

export interface BountyAPI {
    id: number;
    title: string;
    description: string;
    amount: number;
    status: 'OPEN' | 'ASSIGNED' | 'COMPLETED' | 'CANCELLED';
    issueId: number;
}