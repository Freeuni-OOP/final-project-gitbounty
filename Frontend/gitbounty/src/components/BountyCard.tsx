import type { MouseEvent, KeyboardEvent } from 'react';

import type { BountyAPI } from '../types/Bounty';

interface BountyCardProps {
    bounty: BountyAPI;
    authenticated: boolean;
    currentUserId?: number | null;
    isClaiming: boolean;
    isLeaving: boolean;
    onOpen: (bounty: BountyAPI) => void;
    onClaim: (bounty: BountyAPI) => void;
    onLeave: (bounty: BountyAPI) => void;
}

function canNavigateToRepository(bounty: BountyAPI): boolean {
    return Boolean(
        bounty.repositoryOwnerUsername
        && bounty.repositoryName
    );
}

function stopCardClick(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation();
}

const BountyCard = ({
                        bounty,
                        authenticated,
                        currentUserId,
                        isClaiming,
                        isLeaving,
                        onOpen,
                        onClaim,
                        onLeave,
                    }: BountyCardProps) => {
    const isClaimedByCurrentUser =
        authenticated
        && currentUserId !== null
        && currentUserId !== undefined
        && bounty.status === 'ASSIGNED'
        && bounty.assignedToId === currentUserId;

    const isOpen = bounty.status === 'OPEN';
    const canOpen = canNavigateToRepository(bounty);

    const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
        if (!canOpen) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            onOpen(bounty);
        }
    };

    return (
        <article
            className={`bounty-card ${canOpen ? 'clickable' : ''}`}
            role={canOpen ? 'button' : undefined}
            tabIndex={canOpen ? 0 : undefined}
            onClick={() => {
                if (canOpen) {
                    onOpen(bounty);
                }
            }}
            onKeyDown={handleKeyDown}
        >
            <div className="bounty-card-top">
                <div>
                    <p className="bounty-repo-name">
                        {bounty.repositoryOwnerUsername && bounty.repositoryName
                            ? `${bounty.repositoryOwnerUsername}/${bounty.repositoryName}`
                            : 'Repository unavailable'}
                    </p>

                    <h3 className="bounty-issue-title">
                        {bounty.title}
                    </h3>

                    <p className="bounty-issue-line">
                        Issue #{bounty.issueNumber ?? bounty.issueId}
                    </p>
                </div>

                <div className="bounty-reward-badge">
                    {bounty.amount.toLocaleString()} credits
                </div>
            </div>

            {bounty.description && (
                <p className="bounty-description">
                    {bounty.description}
                </p>
            )}

            <div className="bounty-card-meta">
                <span className={`bounty-status-badge status-${bounty.status.toLowerCase()}`}>
                    {bounty.status}
                </span>

                {bounty.assignedToUsername && (
                    <span>
                        Claimed by <strong>{bounty.assignedToUsername}</strong>
                    </span>
                )}
            </div>

            <div className="bounty-card-actions">
                {canOpen && (
                    <button
                        type="button"
                        className="bounty-secondary-btn"
                        onClick={(event) => {
                            stopCardClick(event);
                            onOpen(bounty);
                        }}
                    >
                        View issue
                    </button>
                )}

                {authenticated && isOpen && (
                    <button
                        type="button"
                        className="bounty-primary-btn"
                        disabled={isClaiming}
                        onClick={(event) => {
                            stopCardClick(event);
                            onClaim(bounty);
                        }}
                    >
                        {isClaiming ? 'Claiming...' : 'Claim bounty'}
                    </button>
                )}

                {isClaimedByCurrentUser && (
                    <button
                        type="button"
                        className="bounty-danger-btn"
                        disabled={isLeaving}
                        onClick={(event) => {
                            stopCardClick(event);
                            onLeave(bounty);
                        }}
                    >
                        {isLeaving ? 'Leaving...' : 'Leave bounty'}
                    </button>
                )}
            </div>
        </article>
    );
};

export default BountyCard;