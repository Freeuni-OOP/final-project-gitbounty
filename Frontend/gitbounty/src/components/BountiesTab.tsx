import { useEffect, useMemo, useState } from 'react';

import { useAuth } from '../auth/useAuth';
import { useProfileData } from '../hooks/useProfileData';
import { bountyApi } from '../services/bountyService';
import type { BountyAPI, BountyStatus } from '../types/Bounty';

import '../styles/RepoTabs.css';

type Filter = 'available' | 'assigned' | 'completed' | 'cancelled' | 'all';

interface BountiesTabProps {
    repoId: string;
    onViewIssue?: (issueId: number) => void;
}

function isClaimable(bounty: BountyAPI): boolean {
    return bounty.status === 'OPEN';
}

function isClaimedByCurrentUser(
    bounty: BountyAPI,
    userId: number | null | undefined,
    authenticated: boolean
): boolean {
    return authenticated
        && userId !== null
        && userId !== undefined
        && bounty.status === 'ASSIGNED'
        && bounty.assignedToId === userId;
}

function getShownBounties(
    bounties: BountyAPI[],
    filter: Filter
): BountyAPI[] {
    switch (filter) {
        case 'available':
            return bounties.filter((bounty) => bounty.status === 'OPEN');
        case 'assigned':
            return bounties.filter((bounty) => bounty.status === 'ASSIGNED');
        case 'completed':
            return bounties.filter((bounty) => bounty.status === 'COMPLETED');
        case 'cancelled':
            return bounties.filter((bounty) => bounty.status === 'CANCELLED');
        case 'all':
            return bounties;
        default:
            return bounties;
    }
}

function countByStatus(
    bounties: BountyAPI[],
    status: BountyStatus
): number {
    return bounties.filter((bounty) => bounty.status === status).length;
}

export default function BountiesTab({
                                        repoId,
                                        onViewIssue,
                                    }: BountiesTabProps) {
    const [filter, setFilter] = useState<Filter>('available');
    const [bounties, setBounties] = useState<BountyAPI[]>([]);
    const [loading, setLoading] = useState(true);
    const [claimingBountyId, setClaimingBountyId] = useState<number | null>(null);
    const [unclaimingBountyId, setUnclaimingBountyId] = useState<number | null>(null);
    const [actionError, setActionError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const { authenticated } = useAuth();
    const { user } = useProfileData();

    useEffect(() => {
        let cancelled = false;

        async function fetchBounties() {
            try {
                setLoading(true);
                setActionError(null);

                const data = await bountyApi.getBountiesByRepo(repoId);

                if (!cancelled) {
                    setBounties(data);
                }
            } catch {
                if (!cancelled) {
                    setActionError('Failed to load bounties.');
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        void fetchBounties();

        return () => {
            cancelled = true;
        };
    }, [repoId]);

    useEffect(() => {
        if (!successMessage) {
            return;
        }

        const timer = window.setTimeout(() => {
            setSuccessMessage(null);
        }, 4000);

        return () => {
            window.clearTimeout(timer);
        };
    }, [successMessage]);

    const shownBounties = useMemo(
        () => getShownBounties(bounties, filter),
        [bounties, filter]
    );

    const availableTotal = useMemo(
        () =>
            bounties
                .filter((bounty) => bounty.status === 'OPEN')
                .reduce((sum, bounty) => sum + bounty.amount, 0),
        [bounties]
    );

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

    if (loading) {
        return (
            <div className="tab-panel">
                <div className="tab-empty">
                    Loading bounties...
                </div>
            </div>
        );
    }

    return (
        <div className="tab-panel">
            <div className="tab-panel-header bounties-header-row">
                <div className="bounty-filters">
                    <button
                        type="button"
                        className={`tab-panel-filter ${filter === 'available' ? 'active' : ''}`}
                        onClick={() => setFilter('available')}
                    >
                        Available ({countByStatus(bounties, 'OPEN')})
                    </button>

                    <button
                        type="button"
                        className={`tab-panel-filter ${filter === 'assigned' ? 'active' : ''}`}
                        onClick={() => setFilter('assigned')}
                    >
                        Claimed ({countByStatus(bounties, 'ASSIGNED')})
                    </button>

                    <button
                        type="button"
                        className={`tab-panel-filter ${filter === 'completed' ? 'active' : ''}`}
                        onClick={() => setFilter('completed')}
                    >
                        Completed ({countByStatus(bounties, 'COMPLETED')})
                    </button>

                    <button
                        type="button"
                        className={`tab-panel-filter ${filter === 'cancelled' ? 'active' : ''}`}
                        onClick={() => setFilter('cancelled')}
                    >
                        Cancelled ({countByStatus(bounties, 'CANCELLED')})
                    </button>

                    <button
                        type="button"
                        className={`tab-panel-filter ${filter === 'all' ? 'active' : ''}`}
                        onClick={() => setFilter('all')}
                    >
                        All ({bounties.length})
                    </button>
                </div>

                <span className="bounty-total-label">
                    {availableTotal.toLocaleString()} credits available
                </span>
            </div>

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

            <ul className="tab-item-list bounty-card-list">
                {shownBounties.map((bounty) => {
                    const claimedByMe = isClaimedByCurrentUser(
                        bounty,
                        user?.id,
                        authenticated
                    );

                    const isClaiming = claimingBountyId === bounty.id;
                    const isLeaving = unclaimingBountyId === bounty.id;

                    return (
                        <li
                            key={bounty.id}
                            className={`tab-item bounty-card-item ${onViewIssue ? 'clickable' : ''}`}
                            role={onViewIssue ? 'button' : undefined}
                            tabIndex={onViewIssue ? 0 : undefined}
                            onClick={() => {
                                onViewIssue?.(bounty.issueId);
                            }}
                            onKeyDown={(event) => {
                                if (!onViewIssue) {
                                    return;
                                }

                                if (event.key === 'Enter' || event.key === ' ') {
                                    event.preventDefault();
                                    onViewIssue(bounty.issueId);
                                }
                            }}
                        >
                            <div className="bounty-card-main">
                                <div className="bounty-card-title-row">
                                    <span className="bounty-card-amount">
                                        {bounty.amount.toLocaleString()} credits
                                    </span>

                                    <span className={`bounty-status-badge status-${bounty.status.toLowerCase()}`}>
                                        {bounty.status}
                                    </span>
                                </div>

                                <div className="bounty-card-title">
                                    {bounty.title}
                                </div>

                                {bounty.description && (
                                    <p className="bounty-card-description">
                                        {bounty.description}
                                    </p>
                                )}

                                <div className="bounty-card-meta">
                                    Issue #{bounty.issueId}
                                    {bounty.assignedToUsername && (
                                        <>
                                            {' · '}
                                            claimed by{' '}
                                            <strong>{bounty.assignedToUsername}</strong>
                                        </>
                                    )}
                                </div>
                            </div>

                            <div
                                className="bounty-card-actions"
                                onClick={(event) => {
                                    event.stopPropagation();
                                }}
                            >
                                {authenticated && isClaimable(bounty) && (
                                    <button
                                        type="button"
                                        className="issue-add-bounty-btn"
                                        disabled={isClaiming}
                                        onClick={() => {
                                            void handleClaimBounty(bounty);
                                        }}
                                    >
                                        {isClaiming ? 'Claiming...' : 'Claim bounty'}
                                    </button>
                                )}

                                {claimedByMe && (
                                    <button
                                        type="button"
                                        className="issue-bounty-action-btn cancel"
                                        disabled={isLeaving}
                                        onClick={() => {
                                            void handleUnclaimBounty(bounty);
                                        }}
                                    >
                                        {isLeaving ? 'Leaving...' : 'Leave bounty'}
                                    </button>
                                )}
                            </div>
                        </li>
                    );
                })}

                {shownBounties.length === 0 && (
                    <li className="tab-empty">
                        {filter === 'available'
                            ? 'No available bounties for this repository.'
                            : `No ${filter} bounties for this repository.`}
                    </li>
                )}
            </ul>
        </div>
    );
}