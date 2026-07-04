import { useState, useEffect, useMemo } from 'react';
import PRHeaderControls from './PRHeaderControls';
import PRList from './PRList';
import { CreatePullRequestModal } from '../../CreatePullRequestModal';
import '../../../styles/RepoTabs.css';
import type {Filter, PullRequest} from "../../icons/pullrequest/PRIcons.tsx";
import { pullRequestApi, type CreatePullRequestResponse } from '../../../services/pullRequestService';
import { useAuth } from '../../../auth/useAuth';

export default function PullRequestsTab({ repoName }: Readonly<{ repoName: string }>) {
    const [filter, setFilter] = useState<Filter>('OPEN');
    const [pullRequests, setPullRequests] = useState<PullRequest[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const { isLoading: isAuthLoading, authenticated } = useAuth();

    useEffect(() => {
        if (isAuthLoading) return;
        if (!authenticated) {
            setPullRequests([]);
            setLoading(false);
            setError('You must be logged in to view pull requests');
            return;
        }

        const fetchPullRequests = async () => {
            try {
                setLoading(true);
                setError(null);
                const data = await pullRequestApi.getPullRequests(repoName);
                setPullRequests(data);
            } catch (err: any) {
                const status = err?.response?.status;
                if (status === 401 || status === 403) {
                    setError('Unauthorized. Please sign in again.');
                } else {
                    setError(err instanceof Error ? err.message : 'Failed to fetch pull requests');
                }
                setPullRequests([]);
            } finally {
                setLoading(false);
            }
        };

        fetchPullRequests();
    }, [repoName, isAuthLoading, authenticated]);

    // Memoizing calculations prevents unnecessary re-runs on unrelated state updates
    const counts = useMemo(() => {
        const initialCounts: Record<Filter, number> = { OPEN: 0, MERGED: 0, CLOSED: 0 };
        return pullRequests.reduce((acc, pr) => {
            if (acc[pr.status] !== undefined) acc[pr.status]++;
            return acc;
        }, initialCounts);
    }, [pullRequests]);

    const filteredPRs = useMemo(() => {
        return pullRequests.filter((pr) => pr.status === filter);
    }, [pullRequests, filter]);

    const handleCreatePR = () => {
        setIsCreateModalOpen(true);
    };

    const handlePRCreated = (pr: CreatePullRequestResponse) => {
        // Add the newly created PR to the list
        setPullRequests([pr, ...pullRequests]);
        setIsCreateModalOpen(false);
    };

    return (
        <div className="tab-panel">
            <CreatePullRequestModal
                repositoryName={repoName}
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onCreated={handlePRCreated}
            />

            <PRHeaderControls
                currentFilter={filter}
                onFilterChange={setFilter}
                counts={counts}
                disabled={loading || !!error}
                onCreatePR={handleCreatePR}
            />

            <PRList
                pullRequests={filteredPRs}
                filter={filter}
                loading={loading}
                error={error}
            />
        </div>
    );
}