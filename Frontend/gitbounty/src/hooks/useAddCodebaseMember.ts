import { useState } from 'react';
import apiClient from '../api/apiClient';

export type CodebaseRole = 'MAINTAINER' | 'DEVELOPER' | 'REPORTER';

export interface CodebaseMember {
    id: number;
    username: string;
    email: string;
    role: CodebaseRole | 'OWNER';
}

interface AddCodebaseMemberState {
    addMember: (repositoryName: string, username: string, role: CodebaseRole) => Promise<CodebaseMember>;
    clearError: () => void;
    isSubmitting: boolean;
    error: string | null;
}

export function useAddCodebaseMember(): AddCodebaseMemberState {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const addMember = async (
        repositoryName: string,
        username: string,
        role: CodebaseRole
    ): Promise<CodebaseMember> => {
        setIsSubmitting(true);
        setError(null);

        try {
            const response = await apiClient.post<CodebaseMember>(
                `/api/codebases/${encodeURIComponent(repositoryName)}/members`,
                {
                    username: username.trim(),
                    role,
                }
            );

            return response.data;
        } catch (err: unknown) {
            const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
            const message =
                axiosErr.response?.data?.message ||
                axiosErr.message ||
                'Failed to add member';

            setError(message);
            throw err;
        } finally {
            setIsSubmitting(false);
        }
    };

    const clearError = () => setError(null);

    return { addMember, clearError, isSubmitting, error };
}