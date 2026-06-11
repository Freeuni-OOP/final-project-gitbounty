import type { BountyCardProps } from '../types/Bounty';

const difficultyColors: Record<BountyCardProps['difficulty'], string> = {
    'Good First Issue': '#10b981',
    Medium: '#f59e0b',
    Advanced: '#ef4444',
};

const formatReward = (amount: number, currency: BountyCardProps['currency']): string => {
    if (currency === 'ETH') return `${amount} ETH`;
    return `$${amount.toLocaleString()} ${currency}`;
};

const timeAgo = (dateString: string): string => {
    const days = Math.floor((Date.now() - new Date(dateString).getTime()) / 86400000);
    if (days === 0) return 'today';
    if (days === 1) return '1 day ago';
    return `${days} days ago`;
};

const BountyCard = ({
                        repoName,
                        repoAvatar,
                        issueTitle,
                        rewardAmount,
                        currency,
                        difficulty,
                        languages,
                        createdAt,
                    }: BountyCardProps) => (
    <div className="bounty-card">
        <div className="bounty-card-header">
            <img src={repoAvatar} alt={repoName} className="repo-avatar" />
            <span className="repo-name">{repoName}</span>
            <span className="bounty-time">{timeAgo(createdAt)}</span>
        </div>
        <h3 className="bounty-issue-title">{issueTitle}</h3>
        <div className="bounty-card-footer">
            <div className="bounty-tags">
        <span
            className="difficulty-tag"
            style={{
                backgroundColor: difficultyColors[difficulty] + '20',
                color: difficultyColors[difficulty],
            }}
        >
          {difficulty}
        </span>
                {languages.map((lang) => (
                    <span key={lang} className="language-tag">{lang}</span>
                ))}
            </div>
            <div className="bounty-reward-badge">{formatReward(rewardAmount, currency)}</div>
        </div>
    </div>
);

export default BountyCard;