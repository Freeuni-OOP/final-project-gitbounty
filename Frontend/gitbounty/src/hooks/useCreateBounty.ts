import { useCallback, useState } from 'react';
import { bountyApi } from '../services/bountyService';
import type {
    BountyAPI,
    CreateBountyRequest,
} from '../types/Bounty';

interface UseCreateBountyResult {
    createBounty: (
        request: CreateBountyRequest
    ) => Promise<BountyAPI>;
    clearError: () => void;
    isSubmitting: boolean;
    error: string | null;
}

interface RequestError {
    response?: {
        status?: number;
        data?: {
            message?: string;
        } | string;
    };
    message?: string;
}

function getErrorMessage(error: unknown): string {
    const requestError = error as RequestError;
    const status = requestError.response?.status;

    if (status === 401) {
        return 'Please log in before creating a bounty.';
    }

    if (status === 403) {
        return 'Only the codebase owner can create a bounty.';
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
        'Failed to create bounty. Please try again.'
    );
}

export function useCreateBounty(): UseCreateBountyResult {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const createBounty = async (
        request: CreateBountyRequest
    ): Promise<BountyAPI> => {
        setIsSubmitting(true);
        setError(null);

        try {
            return await bountyApi.createBounty(request);
        } catch (requestError: unknown) {
            setError(getErrorMessage(requestError));
            throw requestError;
        } finally {
            setIsSubmitting(false);
        }
    };

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    return {
        createBounty,
        clearError,
        isSubmitting,
        error,
    };
}