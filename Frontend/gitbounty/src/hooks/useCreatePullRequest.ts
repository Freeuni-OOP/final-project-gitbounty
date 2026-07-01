import { useCallback, useState } from 'react';
import { pullRequestApi, type CreatePullRequestRequest, type CreatePullRequestResponse } from '../services/pullRequestService';

interface UseCreatePullRequestResult {
    createPullRequest: (
        repositoryName: string,
        request: CreatePullRequestRequest
    ) => Promise<CreatePullRequestResponse>;
    clearError: () => void;
    isSubmitting: boolean;
    error: string | null;
}

interface PRRequestError {
    response?: {
        status?: number;
        data?: {
            message?: string;
        } | string;
    };
    message?: string;
}

function getPRErrorMessage(error: unknown): string {
    const requestError = error as PRRequestError;

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
        'Failed to create pull request. Please try again.'
    );
}

export function useCreatePullRequest(): UseCreatePullRequestResult {
    const [isSubmitting, setIsSubmitting] =
        useState(false);

    const [error, setError] =
        useState<string | null>(null);

    const createPullRequest = async (
        repositoryName: string,
        request: CreatePullRequestRequest
    ): Promise<CreatePullRequestResponse> => {
        setIsSubmitting(true);
        setError(null);

        try {
            return await pullRequestApi.createPullRequest(
                repositoryName,
                request
            );
        } catch (requestError: unknown) {
            setError(
                getPRErrorMessage(requestError)
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
        createPullRequest,
        clearError,
        isSubmitting,
        error,
    };
}

