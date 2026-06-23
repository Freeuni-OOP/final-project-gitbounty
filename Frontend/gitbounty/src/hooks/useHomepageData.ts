import { useState, useEffect } from 'react';

const API_BASE = '';

export interface BountyDTO {
    id: number;
    title: string;
    description: string;
    amount: number;
    status: 'OPEN' | 'ASSIGNED' | 'COMPLETED' | 'CANCELLED';
    issueId: number;
}

interface HomepageData {
    bounties: BountyDTO[];
    isLoading: boolean;
    error: string | null;
}

export const useHomepageData = (): HomepageData => {
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [bounties, setBounties] = useState<BountyDTO[]>([]);

    useEffect(() => {
        let cancelled = false;

        const fetchData = async () => {
            try {
                const res = await fetch(`${API_BASE}/api/bounties`);
                if (!res.ok) throw new Error(`Server error: ${res.status}`);
                const data: BountyDTO[] = await res.json();
                if (!cancelled) setBounties(data);
            } catch (err) {
                if (!cancelled) {
                    setError(err instanceof Error ? err.message : 'Failed to load bounties');
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };

        fetchData();
        return () => { cancelled = true; };
    }, []);

    return { bounties, isLoading, error };
};
