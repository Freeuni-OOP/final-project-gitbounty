import { useState, useEffect } from 'react';
import type { BountyCardProps, TrendingTopic, TopRepository, ActivityItem } from '../types/Bounty';
import {
    mockTrendingBounties,
    mockPersonalizedBounties,
    mockTrendingTopics,
    mockTopRepositories,
    mockActivityFeed,
} from '../mocks/homepageMock';

interface HomepageData {
    trendingBounties: BountyCardProps[];
    personalizedBounties: BountyCardProps[];
    trendingTopics: TrendingTopic[];
    topRepositories: TopRepository[];
    activityFeed: ActivityItem[];
    isLoading: boolean;
}

export const useHomepageData = (): HomepageData => {
    const [isLoading, setIsLoading] = useState(true);
    const [data, setData] = useState<Omit<HomepageData, 'isLoading'>>({
        trendingBounties: [],
        personalizedBounties: [],
        trendingTopics: [],
        topRepositories: [],
        activityFeed: [],
    });

    useEffect(() => {
        // TODO: replace with real API calls when backend is ready
        const timer = setTimeout(() => {
            setData({
                trendingBounties: mockTrendingBounties,
                personalizedBounties: mockPersonalizedBounties,
                trendingTopics: mockTrendingTopics,
                topRepositories: mockTopRepositories,
                activityFeed: mockActivityFeed,
            });
            setIsLoading(false);
        }, 300);
        return () => clearTimeout(timer);
    }, []);

    return { ...data, isLoading };
};