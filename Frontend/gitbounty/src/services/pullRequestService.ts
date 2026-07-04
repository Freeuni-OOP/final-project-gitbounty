import apiClient from '../api/apiClient';
import type { PullRequest } from '../components/icons/pullrequest/PRIcons';

export interface CreatePullRequestRequest {
    sourceBranch: string;
    targetBranch: string;
    title: string;
    description?: string | null;
}

export interface CreatePullRequestResponse extends PullRequest {
    description?: string | null;
}

function getPullRequestsPath(repositoryName: string): string {
    return `/api/codebases/${encodeURIComponent(repositoryName)}/pull-requests`;
}

export const pullRequestApi = {
    async getPullRequests(
        repositoryName: string
    ): Promise<CreatePullRequestResponse[]> {
        const response = await apiClient.get<CreatePullRequestResponse[]>(
            getPullRequestsPath(repositoryName)
        );

        return response.data;
    },

    async getPullRequest(
        repositoryName: string,
        prNumber: number
    ): Promise<CreatePullRequestResponse> {
        const response = await apiClient.get<CreatePullRequestResponse>(
            `${getPullRequestsPath(repositoryName)}/${prNumber}`
        );

        return response.data;
    },

    async createPullRequest(
        repositoryName: string,
        request: CreatePullRequestRequest
    ): Promise<CreatePullRequestResponse> {
        const response = await apiClient.post<CreatePullRequestResponse>(
            getPullRequestsPath(repositoryName),
            request
        );

        return response.data;
    },

    async mergePullRequest(
        repositoryName: string,
        prNumber: number
    ): Promise<void> {
        await apiClient.post(
            `${getPullRequestsPath(repositoryName)}/${prNumber}/merge`
        );
    },
};

