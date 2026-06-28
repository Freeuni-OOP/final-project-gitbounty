import { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import { useAuth } from "../auth/useAuth";

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
    // Reactive auth state — re-renders this hook automatically whenever
    // Keycloak's isLoading/authenticated actually changes.
    const { isLoading: isAuthLoading, authenticated } = useAuth();

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [user, setUser] = useState<UserResponse | null>(null);
    const [isUnauthenticated, setIsUnauthenticated] = useState(false);

    useEffect(() => {
        // Wait until auth has genuinely finished initializing —
        // this replaces the old 400ms retryTimer guess.
        if (isAuthLoading) return;

        if (!authenticated) {
            setIsUnauthenticated(true);
            setIsLoading(false);
            return;
        }

        let cancelled = false;

        const fetchProfile = async () => {
            try {
                const res = await apiClient.get('/api/users/profile/me');

                if (!cancelled) {
                    setUser(res.data);
                    setError(null);
                    setIsUnauthenticated(false);
                    setIsLoading(false);
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

        fetchProfile();

        return () => {
            cancelled = true;
        };
    }, [isAuthLoading, authenticated]); // re-runs automatically when real auth state changes

    return { user, isLoading: isLoading || isAuthLoading, error, isUnauthenticated };
};