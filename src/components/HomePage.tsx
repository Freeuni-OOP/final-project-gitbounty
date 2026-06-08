import React, { useState, useEffect } from 'react';
import { BountyCardProps } from '../mocks/homepageMock';
import BountyCard from '../components/BountyCard';
import TrendingTopics from '../components/TrendingTopics';
import TopRewardingRepositories from '../components/TopRewardingRepositories';
import ActivityFeed from '../components/ActivityFeed';

const HomePage = () => {
  const [trendingBounties, setTrendingBounties] = useState<BountyCardProps[]>([]);
  const [recommendedBounties, setRecommendedBounties] = useState<BountyCardProps[]>([]);
  const [trendingTopics, setTrendingTopics] = useState<string[]>([]);
  const [topRewardingRepositories, setTopRewardingRepositories] = useState<any[]>([]);
  const [activityFeed, setActivityFeed] = useState<any[]>([]);

  useEffect(() => {
    // Mock data for now, replace with API calls later
    const trendingBountiesMock: BountyCardProps[] = [
      {
        id: '1',
        repoName: 'react',
        repoAvatar: 'https://avatars.githubusercontent.com/u/1',
        issueTitle: 'Fix bug in React',
        rewardAmount: 500,
        currency: 'USD',
      },
      {
        id: '2',
        repoName: 'nextjs',
        repoAvatar: 'https://avatars.githubusercontent.com/u/2',
        issueTitle: 'Improve performance in Next.js',
        rewardAmount: 1000,
        currency: 'USD',
      },
    ];

    const recommendedBountiesMock: BountyCardProps[] = [
      {
        id: '3',
        repoName: 'typescript',
        repoAvatar: 'https://avatars.githubusercontent.com/u/3',
        issueTitle: 'Add TypeScript support',
        rewardAmount: 200,
        currency: 'USD',
      },
      {
        id: '4',
        repoName: 'rust',
        repoAvatar: 'https://avatars.githubusercontent.com/u/4',
        issueTitle: 'Fix bug in Rust',
        rewardAmount: 300,
        currency: 'USD',
      },
    ];

    const trendingTopicsMock: string[] = ['#NextJS', '#Solidity', '#Wasm'];
    const topRewardingRepositoriesMock: any[] = [
      {
        id: '1',
        repoName: 'react',
        totalReward: 10000,
      },
      {
        id: '2',
        repoName: 'nextjs',
        totalReward: 5000,
      },
    ];

    const activityFeedMock: any[] = [
      {
        id: '1',
        username: 'octocat',
        action: 'claimed a $500 bounty on React',
      },
      {
        id: '2',
        username: 'johnDoe',
        action: 'created a new bounty on Next.js',
      },
    ];

    setTrendingBounties(trendingBountiesMock);
    setRecommendedBounties(recommendedBountiesMock);
    setTrendingTopics(trendingTopicsMock);
    setTopRewardingRepositories(topRewardingRepositoriesMock);
    setActivityFeed(activityFeedMock);
  }, []);

  return (
    <div className="home-page">
      <div className="main-feed">
        <div className="hero-banner">
          <h1>Welcome to GitBounty</h1>
          <button>Explore Bounties</button>
        </div>
        <h2>Trending Bounties</h2>
        <div className="trending-bounties">
          {trendingBounties.map((bounty) => (
            <BountyCard key={bounty.id} {...bounty} />
          ))}
        </div>
        <h2>Based on your interests</h2>
        <div className="recommended-bounties">
          {recommendedBounties.map((bounty) => (
            <BountyCard key={bounty.id} {...bounty} />
          ))}
        </div>
      </div>
      <div className="discovery-sidebar">
        <h2>Trending Topics</h2>
        <TrendingTopics topics={trendingTopics} />
        <h2>Top Rewarding Repositories</h2>
        <TopRewardingRepositories repositories={topRewardingRepositories} />
        <h2>Activity Feed</h2>
        <ActivityFeed activities={activityFeed} />
      </div>
    </div>
  );
};

export default HomePage;