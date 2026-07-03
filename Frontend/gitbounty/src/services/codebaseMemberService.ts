import apiClient from '../api/apiClient';

export type CodebaseRole = 'OWNER' | 'MAINTAINER' | 'DEVELOPER' | 'REPORTER';

export interface CodebaseMemberAPI {
    id: number;
    username: string;
    email: string;
    role: CodebaseRole;
}

function getMembersPath(repositoryName: string): string {
    return `/api/codebases/${encodeURIComponent(repositoryName)}/members`;
}

export const codebaseMemberApi = {
    async getMembers(repositoryName: string): Promise<CodebaseMemberAPI[]> {
        const response = await apiClient.get<CodebaseMemberAPI[]>(
            getMembersPath(repositoryName)
        );

        return response.data;
    },
};