import { useState, useEffect } from 'react';

export interface ApiRepo {
  id: number;
  name: string;
  description: string;
  gitUrl: string;
  ownerUsername: string;
  createdAt: string;
}

interface RepositoriesData {
  repos: ApiRepo[];
  isLoading: boolean;
  error: string | null;
}

export function useRepositoriesData(): RepositoriesData {
  const [repos, setRepos] = useState<ApiRepo[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    fetch('/api/codebases')
      .then(res => {
        if (!res.ok) throw new Error(`${res.status}`);
        return res.json() as Promise<ApiRepo[]>;
      })
      .then(data => {
        if (!cancelled) {
          setRepos(data);
          setIsLoading(false);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
          setIsLoading(false);
        }
      });

    return () => { cancelled = true; };
  }, []);

  return { repos, isLoading, error };
}
