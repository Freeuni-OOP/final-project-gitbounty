import { useState, useEffect } from 'react';
import { bountyApi } from '../services/bountyService';
import type { BountyAPI } from '../types/Bounty';
import '../styles/RepoTabs.css';
import { useAuth } from '../auth/useAuth';
import { useProfileData } from '../hooks/useProfileData';

type Filter = 'all' | BountyAPI['status'];
const FILTERS: Filter[] = ['all', 'OPEN', 'ASSIGNED', 'COMPLETED', 'CANCELLED'];

export default function BountiesTab({ repoId }: { repoId: string }) {
    const [filter, setFilter] = useState<Filter>('all');
    const [bounties, setBounties] = useState<BountyAPI[]>([]);
    const [loading, setLoading] = useState(true);
    const { authenticated } = useAuth();
    const { user } = useProfileData();
    const [claimingBountyId, setClaimingBountyId] = useState<number | null>(null);
    const [actionError, setActionError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [unclaimingBountyId, setUnclaimingBountyId] = useState<number | null>(null);

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

    const handleClaimBounty = async (bounty: BountyAPI) => {
        setClaimingBountyId(bounty.id);
        setActionError(null);
        setSuccessMessage(null);

        try {
            const updatedBounty = await bountyApi.claimBounty(bounty.id);

            setBounties((currentBounties) =>
                currentBounties.map((currentBounty) =>
                    currentBounty.id === updatedBounty.id
                        ? updatedBounty
                        : currentBounty
                )
            );

            setSuccessMessage(`Claimed bounty "${updatedBounty.title}".`);
        } catch {
            setActionError('Failed to claim bounty.');
        } finally {
            setClaimingBountyId(null);
        }
    };

    const handleUnclaimBounty = async (bounty: BountyAPI) => {
        setUnclaimingBountyId(bounty.id);
        setActionError(null);
        setSuccessMessage(null);

        try {
            const updatedBounty = await bountyApi.unclaimBounty(bounty.id);

            setBounties((currentBounties) =>
                currentBounties.map((currentBounty) =>
                    currentBounty.id === updatedBounty.id
                        ? updatedBounty
                        : currentBounty
                )
            );

            setSuccessMessage(`Left bounty "${updatedBounty.title}".`);
        } catch {
            setActionError('Failed to leave bounty.');
        } finally {
            setUnclaimingBountyId(null);
        }
    };

    const shown = filter === 'all'
        ? bounties
        : bounties.filter((b) => b.status === filter);

    const total = bounties.reduce((sum, b) => sum + b.amount, 0);

    if (loading) return <div className="tab-panel">Loading bounties...</div>;

    return (
        <div className="tab-panel">
            {successMessage && (
                <div className="tab-success-banner" role="status">
                    {successMessage}
                </div>
            )}

            {actionError && (
                <div className="tab-error-banner" role="alert">
                    {actionError}
                </div>
            )}

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
                    const isClaimedByCurrentUser =
                        authenticated
                        && user !== null
                        && bounty.status === 'ASSIGNED'
                        && bounty.assignedToId === user.id;
                    return (
                        <li key={bounty.id} className="tab-item bounty-item">
                            <div className="bounty-amount">${bounty.amount}</div>
                            <div className="tab-item-body">
                                <div className="tab-item-title-row">
                                    <span className="tab-item-title">
                                        <span className="bounty-issue-ref">#{bounty.issueId}</span>{' '}
                                        {bounty.title}
                                    </span>

                                    <span className={`bounty-status-badge status-${bounty.status.toLowerCase()}`}>
                                        {bounty.status}
                                    </span>
                                </div>

                                {bounty.assignedToUsername && (
                                    <span className="tab-item-meta">
                                        Claimed by <strong>{bounty.assignedToUsername}</strong>
                                    </span>
                                )}

                                {authenticated && bounty.status === 'OPEN' && (
                                    <button
                                        type="button"
                                        className="issue-add-bounty-btn"
                                        disabled={claimingBountyId === bounty.id}
                                        onClick={() => {
                                            void handleClaimBounty(bounty);
                                        }}
                                    >
                                        {claimingBountyId === bounty.id ? 'Claiming...' : 'Claim bounty'}
                                    </button>
                                )}

                                {isClaimedByCurrentUser && (
                                    <button
                                        type="button"
                                        className="issue-bounty-action-btn cancel"
                                        disabled={unclaimingBountyId === bounty.id}
                                        onClick={() => {
                                            void handleUnclaimBounty(bounty);
                                        }}
                                    >
                                        {unclaimingBountyId === bounty.id ? 'Leaving...' : 'Leave bounty'}
                                    </button>
                                )}

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