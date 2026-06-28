import type { BountyAPI } from '../types/Bounty';

const statusColors: Record<BountyAPI['status'], string> = {
    OPEN: '#10b981',
    ASSIGNED: '#f59e0b',
    COMPLETED: '#3b82f6',
    CANCELLED: '#ef4444',
};

const BountyCard = ({
                        title,
                        description,
                        amount,
                        status,
                        issueId
                    }: BountyAPI) => (
    <div className="bounty-card">
        <div className="bounty-card-header">
            {}
            <div className="repo-avatar" style={{ width: 24, height: 24, borderRadius: '50%', backgroundColor: '#374151' }} />
            <span className="repo-name">Issue #{issueId}</span>
        </div>

        <h3 className="bounty-issue-title">{title}</h3>
        <p style={{ color: '#9ca3af', fontSize: '0.875rem', marginBottom: '1rem', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
            {description}
        </p>

        <div className="bounty-card-footer">
            <div className="bounty-tags">
                <span
                    className="difficulty-tag"
                    style={{
                        backgroundColor: statusColors[status] + '20',
                        color: statusColors[status],
                        padding: '2px 8px',
                        borderRadius: '12px',
                        fontSize: '0.75rem',
                        fontWeight: 'bold'
                    }}
                >
                    {status}
                </span>
            </div>
            <div className="bounty-reward-badge">${amount.toLocaleString()}</div>
        </div>
    </div>
);

export default BountyCard;