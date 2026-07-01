import { useState, useEffect, useMemo } from 'react';
import PRHeaderControls from './PRHeaderControls';
import PRList from './PRList';
import '../../../styles/RepoTabs.css';
import type {Filter} from "../../icons/pullrequest/PRIcons.tsx";
import type {PullRequest} from "../../../mocks/repositoriesMock.ts";
import apiClient from "../../../api/apiClient.ts";

export default function PullRequestsTab({ repoName }: Readonly<{ repoName: string }>) {
    const [filter, setFilter] = useState<Filter>('open');
    const [pullRequests, setPullRequests] = useState<PullRequest[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchPullRequests = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await apiClient.get<PullRequest[]>(
                    `/api/codebases/${repoName}/pull-requests`
                );
                setPullRequests(response.data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to fetch pull requests');
                setPullRequests([]);
            } finally {
                setLoading(false);
            }
        };

        fetchPullRequests();
    }, [repoName]);

    // Memoizing calculations prevents unnecessary re-runs on unrelated state updates
    const counts = useMemo(() => {
        const initialCounts: Record<Filter, number> = { open: 0, merged: 0, closed: 0 };
        return pullRequests.reduce((acc, pr) => {
            if (acc[pr.status] !== undefined) acc[pr.status]++;
            return acc;
        }, initialCounts);
    }, [pullRequests]);

    const filteredPRs = useMemo(() => {
        return pullRequests.filter((pr) => pr.status === filter);
    }, [pullRequests, filter]);

    const handleCreatePR = () => {
        // Add your routing/modal logic here for creating a PR
        console.log(`Initiating PR creation sequence for ${repoName}`);
    };

    return (
        <div className="tab-panel">
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