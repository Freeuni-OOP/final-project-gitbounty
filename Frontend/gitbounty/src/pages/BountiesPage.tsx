import { useState } from 'react';
import BountyCard from '../components/BountyCard';
import { mockTrendingBounties, mockPersonalizedBounties } from '../mocks/homepageMock';
import type { BountyCardProps } from '../types/Bounty';
import '../styles/BountiesPage.css';

const ALL_BOUNTIES = [...mockTrendingBounties, ...mockPersonalizedBounties];

const DIFFICULTIES: Array<BountyCardProps['difficulty'] | 'All'> = [
    'All',
    'Good First Issue',
    'Medium',
    'Advanced',
];

const BountiesPage = () => {
    const [difficulty, setDifficulty] = useState<BountyCardProps['difficulty'] | 'All'>('All');

    const filtered = difficulty === 'All'
        ? ALL_BOUNTIES
        : ALL_BOUNTIES.filter((b) => b.difficulty === difficulty);

    return (
        <div className="bounties-page">
            <div className="bounties-header">
                <h1 className="bounties-title">Browse Bounties</h1>
                <p className="bounties-subtitle">{filtered.length} open bounties</p>
            </div>

            <div className="bounties-filters">
                {DIFFICULTIES.map((d) => (
                    <button
                        key={d}
                        className={`filter-pill ${difficulty === d ? 'active' : ''}`}
                        onClick={() => setDifficulty(d)}
                    >
                        {d}
                    </button>
                ))}
            </div>

            <div className="bounties-list">
                {filtered.map((bounty) => (
                    <BountyCard key={bounty.id} {...bounty} />
                ))}
            </div>
        </div>
    );
};

export default BountiesPage;
