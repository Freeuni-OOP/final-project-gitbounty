import { useEffect, useMemo, useState } from 'react';

import { bountyApi } from '../services/bountyService';
import type { BountyAPI } from '../types/Bounty';

type BountyTab = 'My repositories' | 'claimed';

function isActiveBounty(bounty: BountyAPI): boolean {
    return bounty.status === 'OPEN' || bounty.status === 'ASSIGNED';
}

export function ProfileBountiesSection() {
    const [activeTab, setActiveTab] = useState<BountyTab>('My repositories');
    const [repositoryBounties, setrepositoryBounties] = useState<BountyAPI[]>([]);
    const [claimedBounties, setClaimedBounties] = useState<BountyAPI[]>([]);
    const [loading, setLoading] = useState(true);
    const [actionBountyId, setActionBountyId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function loadBounties() {
            try {
                setLoading(true);
                setError(null);

                const [posted, claimed] = await Promise.all([
                    bountyApi.getMyRepositoryBounties(),
                    bountyApi.getMyClaimedBounties(),
                ]);

                if (!cancelled) {
                    setrepositoryBounties(posted);
                    setClaimedBounties(claimed);
                }
            } catch {
                if (!cancelled) {
                    setError('Failed to load your bounties.');
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        void loadBounties();

        return () => {
            cancelled = true;
        };
    }, []);

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

    const shownBounties = activeTab === 'My repositories'
        ? repositoryBounties
        : claimedBounties;

    const repositoryTotal = useMemo(
        () => repositoryBounties.reduce((sum, bounty) => sum + bounty.amount, 0),
        [repositoryBounties]
    );

    const claimedTotal = useMemo(
        () => claimedBounties.reduce((sum, bounty) => sum + bounty.amount, 0),
        [claimedBounties]
    );

    const handleCancelBounty = async (bounty: BountyAPI) => {
        const confirmed = window.confirm(
            `Cancel bounty "${bounty.title}"? ${bounty.amount.toLocaleString()} credits will be refunded.`
        );

        if (!confirmed) {
            return;
        }

        setActionBountyId(bounty.id);
        setError(null);
        setSuccessMessage(null);

        try {
            await bountyApi.cancelBounty(bounty.id);

            setrepositoryBounties((current) =>
                current.map((currentBounty) =>
                    currentBounty.id === bounty.id
                        ? { ...currentBounty, status: 'CANCELLED' }
                        : currentBounty
                )
            );

            setClaimedBounties((current) =>
                current.map((currentBounty) =>
                    currentBounty.id === bounty.id
                        ? { ...currentBounty, status: 'CANCELLED' }
                        : currentBounty
                )
            );

            setSuccessMessage(`Cancelled bounty "${bounty.title}".`);
        } catch {
            setError('Failed to cancel bounty.');
        } finally {
            setActionBountyId(null);
        }
    };

    const handleLeaveBounty = async (bounty: BountyAPI) => {
        setActionBountyId(bounty.id);
        setError(null);
        setSuccessMessage(null);

        try {
            const updatedBounty = await bountyApi.unclaimBounty(bounty.id);

            setClaimedBounties((current) =>
                current.filter((currentBounty) => currentBounty.id !== bounty.id)
            );

            setrepositoryBounties((current) =>
                current.map((currentBounty) =>
                    currentBounty.id === updatedBounty.id
                        ? updatedBounty
                        : currentBounty
                )
            );

            setSuccessMessage(`Left bounty "${bounty.title}".`);
        } catch {
            setError('Failed to leave bounty.');
        } finally {
            setActionBountyId(null);
        }
    };

    return (
        <section className="profile-bounties-section">
            <div className="profile-bounties-header">
                <div>
                    <h2 className="profile-bounties-title">Bounties</h2>
                    <p className="profile-bounties-subtitle">
                        Track bounties on your repositories and bounties you claimed.
                    </p>
                </div>

                <div className="profile-bounty-tabs">
                    <button
                        type="button"
                        className={`profile-bounty-tab ${activeTab === 'My repositories' ? 'active' : ''}`}
                        onClick={() => setActiveTab('My repositories')}
                    >
                        Posted ({repositoryBounties.length})
                    </button>

                    <button
                        type="button"
                        className={`profile-bounty-tab ${activeTab === 'claimed' ? 'active' : ''}`}
                        onClick={() => setActiveTab('claimed')}
                    >
                        Claimed ({claimedBounties.length})
                    </button>
                </div>
            </div>

            <div className="profile-bounty-summary">
                <span>
                    Posted total: <strong>{repositoryTotal.toLocaleString()} credits</strong>
                </span>
                <span>
                    Claimed total: <strong>{claimedTotal.toLocaleString()} credits</strong>
                </span>
            </div>

            {successMessage && (
                <div className="profile-bounty-success" role="status">
                    {successMessage}
                </div>
            )}

            {error && (
                <div className="profile-bounty-error" role="alert">
                    {error}
                </div>
            )}

            {loading ? (
                <p className="profile-bounty-status">Loading bounties…</p>
            ) : shownBounties.length === 0 ? (
                <div className="profile-bounty-empty">
                    {activeTab === 'My repositories'
                        ? 'There are no bounties on your repositories yet.'
                        : 'You have not claimed any bounties yet.'}
                </div>
            ) : (
                <div className="profile-bounty-list">
                    {shownBounties.map((bounty) => {
                        const isRunningAction = actionBountyId === bounty.id;

                        return (
                            <article key={bounty.id} className="profile-bounty-card">
                                <div className="profile-bounty-main">
                                    <div className="profile-bounty-title-row">
                                        <h3 className="profile-bounty-card-title">
                                            {bounty.title}
                                        </h3>

                                        <span className={`profile-bounty-status-badge status-${bounty.status.toLowerCase()}`}>
                                            {bounty.status}
                                        </span>
                                    </div>

                                    <p className="profile-bounty-meta">
                                        Issue #{bounty.issueId}
                                        {bounty.assignedToUsername && (
                                            <>
                                                {' · '}
                                                Claimed by <strong>{bounty.assignedToUsername}</strong>
                                            </>
                                        )}
                                    </p>

                                    {bounty.description && (
                                        <p className="profile-bounty-description">
                                            {bounty.description}
                                        </p>
                                    )}
                                </div>

                                <div className="profile-bounty-actions">
                                    <div className="profile-bounty-amount">
                                        {bounty.amount.toLocaleString()} credits
                                    </div>

                                    {activeTab === 'My repositories' && isActiveBounty(bounty) && (
                                        <button
                                            type="button"
                                            className="profile-bounty-action danger"
                                            disabled={isRunningAction}
                                            onClick={() => {
                                                void handleCancelBounty(bounty);
                                            }}
                                        >
                                            {isRunningAction ? 'Cancelling…' : 'Cancel bounty'}
                                        </button>
                                    )}

                                    {activeTab === 'claimed' && bounty.status === 'ASSIGNED' && (
                                        <button
                                            type="button"
                                            className="profile-bounty-action"
                                            disabled={isRunningAction}
                                            onClick={() => {
                                                void handleLeaveBounty(bounty);
                                            }}
                                        >
                                            {isRunningAction ? 'Leaving…' : 'Leave bounty'}
                                        </button>
                                    )}
                                </div>
                            </article>
                        );
                    })}
                </div>
            )}
        </section>
    );
}