import { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface BranchData {
  id: number;
  name: string;
  latestCommitId: number | null;
}

interface CodebaseWithBranches {
  id: number;
  name: string;
  description: string;
  gitUrl: string;
  ownerUsername: string;
  createdAt: string;
  branches: BranchData[];
}

interface UseBranchesResult {
  branches: string[];
  isLoading: boolean;
  error: string | null;
}

export function useBranches(repositoryName: string): UseBranchesResult {
  const [branches, setBranches] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!repositoryName) {
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setError(null);

    apiClient
      .get<CodebaseWithBranches>(`/api/codebases/${repositoryName}`)
      .then((response) => {
        if (!cancelled) {
          const branchNames =
            response.data.branches?.map((branch) => branch.name) || [];
          setBranches(branchNames);
          setIsLoading(false);
        }
      })
      .catch((err: any) => {
        if (!cancelled) {
          setError(err.response?.status?.toString() || err.message);
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [repositoryName]);

  return { branches, isLoading, error };
}


