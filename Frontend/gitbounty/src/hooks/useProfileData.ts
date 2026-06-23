import { useState, useEffect } from 'react';

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

        const fetchProfile = async () => {
            try {
                const res = await fetch('/api/users/profile/me');
                if (res.status === 401 || res.status === 403) {
                    if (!cancelled) setIsUnauthenticated(true);
                    return;
                }
                if (!res.ok) throw new Error(`Server error: ${res.status}`);
                const data: UserResponse = await res.json();
                if (!cancelled) setUser(data);
            } catch (err) {
                if (!cancelled) {
                    setError(err instanceof Error ? err.message : 'Failed to load profile');
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };

        fetchProfile();
        return () => { cancelled = true; };
    }, []);

    return { user, isLoading, error, isUnauthenticated };
};
