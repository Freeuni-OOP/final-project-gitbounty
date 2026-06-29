import { useState, useEffect } from 'react';
import apiClient from "../api/apiClient.ts";

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
    apiClient.get<ApiRepo[]>('/api/codebases')
        .then(response => {
          // Axios automatically parses JSON and throws on 4xx/5xx responses
          if (!cancelled) {
            setRepos(response.data);
            setIsLoading(false);
          }
        })
        .catch((err) => {
          if (!cancelled) {
            // Axios errors put the message under err.message
            // If the server returned an error body, it lives under err.response?.data
            setError(err.message);
            setIsLoading(false);
          }
        });

    return () => { cancelled = true; };
  }, []);

  return { repos, isLoading, error };
}
