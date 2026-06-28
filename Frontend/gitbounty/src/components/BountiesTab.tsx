import { useState, useEffect } from 'react';
import { bountyApi } from '../services/bountyService';
import type { BountyAPI } from '../types/Bounty';
import '../styles/RepoTabs.css';

type Filter = 'all' | BountyAPI['status'];
const FILTERS: Filter[] = ['all', 'OPEN', 'ASSIGNED', 'COMPLETED', 'CANCELLED'];

export default function BountiesTab({ repoId }: { repoId: string }) {
    const [filter, setFilter] = useState<Filter>('all');
    const [bounties, setBounties] = useState<BountyAPI[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchBounties() {
            try {
                setLoading(true);
                const data = await bountyApi.getBountiesByRepo(repoId);
                setBounties(data);
            } catch (error) {
                console.error("Failed to load bounties:", error);
            } finally {
                setLoading(false);
            }
        }
        fetchBounties();
    }, [repoId]);

    const shown = filter === 'all'
        ? bounties
        : bounties.filter((b) => b.status === filter);

    const total = bounties.reduce((sum, b) => sum + b.amount, 0);

    if (loading) return <div className="tab-panel">Loading bounties...</div>;

    return (
        <div className="tab-panel">
            <div className="tab-panel-header bounties-header-row">
                <div className="bounty-filters">
                    {FILTERS.map((f) => (
                        <button
                            key={f}
                            className={`tab-panel-filter ${filter === f ? 'active' : ''}`}
                            onClick={() => setFilter(f)}
                        >
                            {f === 'all'
                                ? `All (${bounties.length})`
                                : `${f.charAt(0).toUpperCase() + f.slice(1)} (${bounties.filter((b) => b.status === f).length})`}
                        </button>
                    ))}
                </div>
                <span className="bounty-total-label">${total.toLocaleString()} total posted</span>
            </div>

            <ul className="tab-item-list">
                {shown.map((bounty) => {
                    return (
                        <li key={bounty.id} className="tab-item bounty-item">
                            <div className="bounty-amount">${bounty.amount}</div>
                            <div className="tab-item-body">
                                <div className="tab-item-title-row">
                                    <span className="tab-item-title">
                                        <span className="bounty-issue-ref">#{bounty.issueId}</span>{' '}
                                        {bounty.title}
                                    </span>

                                    {/* CLEAN MODIFIER CLASS FIX APPLIED HERE */}
                                    <span className={`bounty-status-badge status-${bounty.status.toLowerCase()}`}>
                                        {bounty.status}
                                    </span>
                                </div>
                                <div className="tab-item-meta">
                                    Click to view detailed reward conditions
                                </div>
                            </div>
                        </li>
                    );
                })}
                {shown.length === 0 && (
                    <li className="tab-empty">No {filter === 'all' ? '' : filter} bounties found for this codebase.</li>
                )}
            </ul>
        </div>
    );
}