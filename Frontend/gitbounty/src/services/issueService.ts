import apiClient from '../api/apiClient';
import type {
    CreateIssueRequest,
    IssueAPI,
} from '../types/Issue';

function getIssuesPath(repositoryName: string): string {
    return `/api/codebases/${encodeURIComponent(repositoryName)}/issues`;
}

export const issueApi = {
    async getIssues(
        repositoryName: string
    ): Promise<IssueAPI[]> {
        const response = await apiClient.get<IssueAPI[]>(
            getIssuesPath(repositoryName)
        );

        return response.data;
    },

    async getIssue(
        repositoryName: string,
        issueNumber: number
    ): Promise<IssueAPI> {
        const response = await apiClient.get<IssueAPI>(
            `${getIssuesPath(repositoryName)}/${issueNumber}`
        );

        return response.data;
    },

    async createIssue(
        repositoryName: string,
        request: CreateIssueRequest
    ): Promise<IssueAPI> {
        const response = await apiClient.post<IssueAPI>(
            getIssuesPath(repositoryName),
            request
        );

        return response.data;
    },
};