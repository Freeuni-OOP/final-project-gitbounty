import { useCallback, useState } from 'react';
import { issueApi } from '../services/issueService';
import type {
    CreateIssueRequest,
    IssueAPI,
} from '../types/Issue';

interface UseCreateIssueResult {
    createIssue: (
        repositoryName: string,
        request: CreateIssueRequest
    ) => Promise<IssueAPI>;
    clearError: () => void;
    isSubmitting: boolean;
    error: string | null;
}

interface IssueRequestError {
    response?: {
        status?: number;
        data?: {
            message?: string;
        } | string;
    };
    message?: string;
}

function getIssueErrorMessage(error: unknown): string {
    const requestError = error as IssueRequestError;
    const status = requestError.response?.status;

    if (status === 401) {
        return 'Please log in before creating an issue.';
    }

    if (status === 403) {
        return 'You do not have permission to create this issue.';
    }

    if (status === 404) {
        return 'The selected codebase could not be found.';
    }

    const responseData = requestError.response?.data;

    if (
        typeof responseData === 'string' &&
        responseData.trim()
    ) {
        return responseData;
    }

    if (
        typeof responseData === 'object' &&
        responseData?.message
    ) {
        return responseData.message;
    }

    return (
        requestError.message ??
        'Failed to create issue. Please try again.'
    );
}

export function useCreateIssue(): UseCreateIssueResult {
    const [isSubmitting, setIsSubmitting] =
        useState(false);

    const [error, setError] =
        useState<string | null>(null);

    const createIssue = async (
        repositoryName: string,
        request: CreateIssueRequest
    ): Promise<IssueAPI> => {
        setIsSubmitting(true);
        setError(null);

        try {
            return await issueApi.createIssue(
                repositoryName,
                request
            );
        } catch (requestError: unknown) {
            setError(
                getIssueErrorMessage(requestError)
            );

            throw requestError;
        } finally {
            setIsSubmitting(false);
        }
    };

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    return {
        createIssue,
        clearError,
        isSubmitting,
        error,
    };
}