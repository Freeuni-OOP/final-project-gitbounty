import { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

export interface UserResponse {
    id: number;
    username: string;
    email: string;
    // Spring Boot serializes LocalDateTime as a numeric array by default:
    // [year, month, day, hour, minute, second, nano]
    // but handle ISO string too in case config changes.
    createdAt: string | number[];
}

interface ProfileData {
    user: UserResponse | null;
    isLoading: boolean;
    error: string | null;
    isUnauthenticated: boolean;
}

export function parseCreatedAt(raw: string | number[]): Date {
    if (Array.isArray(raw)) {
        const [year, month, day] = raw;
        return new Date(year, month - 1, day);
    }
    return new Date(raw);
}

export const useProfileData = (): ProfileData => {
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [user, setUser] = useState<UserResponse | null>(null);
    const [isUnauthenticated, setIsUnauthenticated] = useState(false);

    useEffect(() => {
        let cancelled = false;
        let successfullyLoaded = false;

        const fetchProfile = async () => {
            try {
                const res = await apiClient.get('/api/users/profile/me');

                if (!cancelled) {
                    setUser(res.data);
                    setError(null);
                    setIsUnauthenticated(false);
                    setIsLoading(false);
                    successfullyLoaded = true;
                }
            } catch (err: any) {
                if (!cancelled) {
                    const status = err.response?.status;
                    if (status === 401 || status === 403) {
                        setIsUnauthenticated(true);
                    } else {
                        setError(err.response?.data?.message || err.message || 'Failed to load profile');
                    }
                    setIsLoading(false);
                }
            }
        };

        // Initial profile request on mount
        fetchProfile();

        // Tab-Refresh safety rule: If a user hard-refreshes the browser directly on /profile,
        // retry the query a fraction of a second later in case auth provider was still setting up session keys.
        const retryTimer = setTimeout(() => {
            if (!cancelled && !successfullyLoaded) {
                fetchProfile();
            }
        }, 400);

        return () => {
            cancelled = true;
            clearTimeout(retryTimer);
        };
    }, []);

    return { user, isLoading, error, isUnauthenticated };
};
