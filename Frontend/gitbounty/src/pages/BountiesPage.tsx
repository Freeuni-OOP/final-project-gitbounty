import { useState, useEffect } from 'react';
import BountyCard from '../components/BountyCard';
import { bountyApi } from '../services/bountyService';
import type { BountyAPI } from '../types/Bounty';
import '../styles/BountiesPage.css';

// Change filters to match your backend ENUM
const STATUSES: Array<BountyAPI['status'] | 'All'> = [
    'All',
    'OPEN',
    'ASSIGNED',
    'COMPLETED'
];

const BountiesPage = () => {
    const [bounties, setBounties] = useState<BountyAPI[]>([]);
    // Update state to use Status instead of Difficulty
    const [statusFilter, setStatusFilter] = useState<BountyAPI['status'] | 'All'>('All');
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        bountyApi.getAllBounties()
            .then((data) => {
                setBounties(data);
                setIsLoading(false);
            })
            .catch((err) => {
                console.error("Error fetching global bounties:", err);
                setError("Failed to load active bounties from server.");
                setIsLoading(false);
            });
    }, []);

    // Filter against the real backend 'status' field
    const filtered = statusFilter === 'All'
        ? bounties
        : bounties.filter((b) => b.status === statusFilter);

    if (isLoading) return (
        <div
            className="bounties-loading"
            style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '50vh'
            }}
        >
            Loading live bounties...
        </div>
    );

    if (error) return (
        <div
            className="bounties-error"
            style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '50vh',
                color: '#ef4444',
                fontWeight: '500'
            }}
        >
            {error}
        </div>
    );

    return (
        <div className="bounties-page">
            <div className="bounties-header">
                <h1 className="bounties-title">Browse Bounties</h1>
                <p className="bounties-subtitle">{filtered.length} bounties</p>
            </div>

            <div className="bounties-filters">
                {STATUSES.map((s) => (
                    <button
                        key={s}
                        className={`filter-pill ${statusFilter === s ? 'active' : ''}`}
                        onClick={() => setStatusFilter(s)}
                    >
                        {s}
                    </button>
                ))}
            </div>

            <div className="bounties-list">
                {filtered.length === 0 ? (
                    <p className="no-bounties">No bounties found matching this status.</p>
                ) : (
                    filtered.map((bounty) => (
                        <BountyCard key={bounty.id} {...bounty} />
                    ))
                )}
            </div>
        </div>
    );
};

export default BountiesPage;