import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PRIcon } from '../components/icons/pullrequest/PRIcons';
import '../styles/PRPage.css';
import { pullRequestApi, type CreatePullRequestResponse } from '../services/pullRequestService';
import { useAuth } from '../auth/useAuth';
import { useProfileData } from '../hooks/useProfileData';
import FilesChangedTab from '../components/tabs/pullrequests/FilesChangedTab';

type PRPageData = CreatePullRequestResponse;

type PRTab = 'Conversation' | 'Files changed';
const PR_TABS: PRTab[] = ['Conversation', 'Files changed'];

export default function PRPage() {
    const { owner = '', repoName = '', prNumber = '' } = useParams<{
        owner: string;
        repoName: string;
        prNumber: string;
    }>();

    const [pullRequest, setPullRequest] = useState<PRPageData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isMerging, setIsMerging] = useState(false);
    const [mergeError, setMergeError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<PRTab>('Conversation');
    const { isLoading: isAuthLoading, authenticated } = useAuth();
    const { user: currentUser, isLoading: isProfileLoading } = useProfileData();

    useEffect(() => {
        if (!repoName || !prNumber || isAuthLoading) return;

        if (!authenticated) {
            setLoading(false);
            setError('You must be logged in to view this pull request.');
            return;
        }

        let cancelled = false;
        setLoading(true);
        setError(null);

        const fetchPR = async () => {
            try {
                const prNum = Number.parseInt(prNumber, 10);
                if (Number.isNaN(prNum)) {
                    setError('Invalid PR number');
                    return;
                }

                const data = await pullRequestApi.getPullRequest(repoName, prNum);
                if (!cancelled) {
                    setPullRequest(data);
                }
            } catch (err: any) {
                if (!cancelled) {
                    const status = err?.response?.status;
                    if (status === 404) {
                        setError('Pull request not found.');
                    } else if (status === 401 || status === 403) {
                        setError('Unauthorized. Please sign in again.');
                    } else {
                        setError(err.message || 'Failed to load pull request');
                    }
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        };

        fetchPR();

        return () => {
            cancelled = true;
        };
    }, [repoName, prNumber, isAuthLoading, authenticated]);

    const canMerge =
        authenticated &&
        !isAuthLoading &&
        !isProfileLoading &&
        currentUser?.username === owner &&
        pullRequest?.status === 'OPEN';

    const handleMerge = async () => {
        if (!pullRequest) return;

        const prNum = Number.parseInt(prNumber, 10);
        if (Number.isNaN(prNum)) {
            setMergeError('Invalid PR number');
            return;
        }

        setIsMerging(true);
        setMergeError(null);

        try {
            await pullRequestApi.mergePullRequest(repoName, prNum);
            setPullRequest((prev) => (prev ? { ...prev, status: 'MERGED' } : prev));
        } catch (err: any) {
            const status = err?.response?.status;
            if (status === 403) {
                setMergeError('Only the repository owner can merge this pull request.');
            } else if (status === 404) {
                setMergeError('Pull request not found.');
            } else {
                setMergeError(err?.response?.data?.message || err?.message || 'Failed to merge pull request');
            }
        } finally {
            setIsMerging(false);
        }
    };

    if (loading) {
        return (
            <div className="pr-page">
                <div className="pr-loading">
                    <p>Loading pull request…</p>
                </div>
            </div>
        );
    }

    if (error || !pullRequest) {
        return (
            <div className="pr-page">
                <div className="pr-error">
                    <p>
                        {error ? `Error: ${error}` : 'Pull request not found.'}
                    </p>
                    <Link to={`/repositories/${owner}/${repoName}`} className="back-link">
                        Back to repository
                    </Link>
                </div>
            </div>
        );
    }

    const statusBadgeClass = `status-badge ${pullRequest.status.toLowerCase()}`;
    const formattedDate = new Date(pullRequest.createdAt).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
    });

    return (
        <div className="pr-page">
            <div className="pr-header">
                <div className="pr-breadcrumb">
                    <Link to="/repositories" className="breadcrumb-link">
                        Repositories
                    </Link>
                    <span className="breadcrumb-sep">/</span>
                    <Link to={`/repositories/${owner}/${repoName}`} className="breadcrumb-link">
                        {owner}/{repoName}
                    </Link>
                    <span className="breadcrumb-sep">/</span>
                    <span className="breadcrumb-current">Pull Request #{pullRequest.number}</span>
                </div>

                <div className="pr-title-row">
                    <PRIcon status={pullRequest.status} width={24} height={24} />
                    <h1 className="pr-title">{pullRequest.title}</h1>
                    <span className={statusBadgeClass}>{pullRequest.status}</span>
                </div>

                <div className="pr-meta-row">
                    <span className="pr-number">#{pullRequest.number}</span>
                    <span className="pr-author">
                        opened {formattedDate} by <strong>{pullRequest.authorUsername}</strong>
                    </span>
                </div>

                <div className="pr-tabs">
                    {PR_TABS.map((tab) => (
                        <button
                            key={tab}
                            type="button"
                            className={`pr-tab ${activeTab === tab ? 'active' : ''}`}
                            onClick={() => setActiveTab(tab)}
                        >
                            {tab}
                        </button>
                    ))}
                </div>
            </div>

            {activeTab === 'Files changed' && (
                <FilesChangedTab repositoryName={repoName} prNumber={pullRequest.number} />
            )}

            {activeTab === 'Conversation' && (
                <div className="pr-content">
                    <div className="pr-info-section">
                        <div className="pr-branches">
                            <div className="branch-info">
                                <span className="branch-label">Source Branch</span>
                                <span className="branch-name">{pullRequest.sourceBranch}</span>
                            </div>
                            <div className="branch-arrow">→</div>
                            <div className="branch-info">
                                <span className="branch-label">Target Branch</span>
                                <span className="branch-name">{pullRequest.targetBranch}</span>
                            </div>
                        </div>
                    </div>

                    {pullRequest.description && (
                        <div className="pr-description-section">
                            <h2>Description</h2>
                            <div className="pr-description">
                                {pullRequest.description}
                            </div>
                        </div>
                    )}

                    <div className="pr-footer">
                        {mergeError && <p className="pr-action-error">{mergeError}</p>}
                        {canMerge && (
                            <button
                                type="button"
                                className="merge-button"
                                onClick={handleMerge}
                                disabled={isMerging}
                            >
                                {isMerging ? 'Merging…' : 'Merge pull request'}
                            </button>
                        )}
                        <Link
                            to={`/repositories/${owner}/${repoName}`}
                            className="back-button"
                        >
                            ← Back to repository
                        </Link>
                    </div>
                </div>
            )}
        </div>
    );
}


