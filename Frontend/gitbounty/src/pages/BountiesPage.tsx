import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuth } from '../auth/useAuth';
import BountyCard from '../components/BountyCard';
import { useProfileData } from '../hooks/useProfileData';
import { bountyApi } from '../services/bountyService';
import type { BountyAPI } from '../types/Bounty';

import '../styles/BountiesPage.css';

type Filter = 'OPEN' | 'ASSIGNED' | 'COMPLETED' | 'CANCELLED' | 'All';

const STATUSES: Filter[] = [
    'OPEN',
    'ASSIGNED',
    'COMPLETED',
    'CANCELLED',
    'All',
];

function getFilterLabel(status: Filter): string {
    if (status === 'OPEN') {
        return 'Available';
    }

    if (status === 'ASSIGNED') {
        return 'Claimed';
    }

    if (status === 'All') {
        return 'All';
    }

    return status.charAt(0) + status.slice(1).toLowerCase();
}

const BountiesPage = () => {
    const [bounties, setBounties] = useState<BountyAPI[]>([]);
    const [statusFilter, setStatusFilter] = useState<Filter>('OPEN');
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [claimingBountyId, setClaimingBountyId] = useState<number | null>(null);
    const [leavingBountyId, setLeavingBountyId] = useState<number | null>(null);

    const navigate = useNavigate();
    const { authenticated } = useAuth();
    const { user } = useProfileData();

    useEffect(() => {
        let cancelled = false;

        async function fetchBounties() {
            try {
                setIsLoading(true);
                setError(null);

                const data = await bountyApi.getAllBounties();

                if (!cancelled) {
                    setBounties(data);
                }
            } catch (err) {
                console.error('Error fetching global bounties:', err);

                if (!cancelled) {
                    setError('Failed to load bounties from server.');
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        }

        void fetchBounties();

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

    const filtered = useMemo(
        () =>
            statusFilter === 'All'
                ? bounties
                : bounties.filter((bounty) => bounty.status === statusFilter),
        [bounties, statusFilter]
    );

    const availableTotal = useMemo(
        () =>
            bounties
                .filter((bounty) => bounty.status === 'OPEN')
                .reduce((sum, bounty) => sum + bounty.amount, 0),
        [bounties]
    );

    const handleOpenBounty = (bounty: BountyAPI) => {
        if (!bounty.repositoryOwnerUsername || !bounty.repositoryName) {
            return;
        }

        const owner = encodeURIComponent(bounty.repositoryOwnerUsername);
        const repo = encodeURIComponent(bounty.repositoryName);

        navigate(
            `/repositories/${owner}/${repo}?tab=Issues&issue=${bounty.issueNumber ?? bounty.issueId}`
        );
    };

    const handleClaimBounty = async (bounty: BountyAPI) => {
        setClaimingBountyId(bounty.id);
        setError(null);
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
            setError('Failed to claim bounty.');
        } finally {
            setClaimingBountyId(null);
        }
    };

    const handleLeaveBounty = async (bounty: BountyAPI) => {
        setLeavingBountyId(bounty.id);
        setError(null);
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
            setError('Failed to leave bounty.');
        } finally {
            setLeavingBountyId(null);
        }
    };

    if (isLoading) {
        return (
            <div className="bounties-loading">
                Loading live bounties...
            </div>
        );
    }

    return (
        <div className="bounties-page">
            <div className="bounties-header">
                <div>
                    <h1 className="bounties-title">Browse Bounties</h1>
                    <p className="bounties-subtitle">
                        Find issues with rewards and claim the ones you want to work on.
                    </p>
                </div>

                <div className="bounties-summary-card">
                    <span>{availableTotal.toLocaleString()} credits available</span>
                    <strong>{bounties.length} total bounties</strong>
                </div>
            </div>

            <div className="bounties-filters">
                {STATUSES.map((status) => (
                    <button
                        key={status}
                        type="button"
                        className={`filter-pill ${statusFilter === status ? 'active' : ''}`}
                        onClick={() => setStatusFilter(status)}
                    >
                        {getFilterLabel(status)}
                    </button>
                ))}
            </div>

            {successMessage && (
                <div className="bounties-success" role="status">
                    {successMessage}
                </div>
            )}

            {error && (
                <div className="bounties-error" role="alert">
                    {error}
                </div>
            )}

            <div className="bounties-list">
                {filtered.length === 0 ? (
                    <p className="no-bounties">
                        No bounties found matching this status.
                    </p>
                ) : (
                    filtered.map((bounty) => (
                        <BountyCard
                            key={bounty.id}
                            bounty={bounty}
                            authenticated={authenticated}
                            currentUserId={user?.id}
                            isClaiming={claimingBountyId === bounty.id}
                            isLeaving={leavingBountyId === bounty.id}
                            onOpen={handleOpenBounty}
                            onClaim={handleClaimBounty}
                            onLeave={handleLeaveBounty}
                        />
                    ))
                )}
            </div>
        </div>
    );
};

export default BountiesPage;