import { useState } from 'react';
import apiClient from '../api/apiClient';
import type { ApiRepo } from './useRepositoriesData';

interface CreateRepositoryState {
  createRepository: (name: string, description: string) => Promise<ApiRepo>;
  clearError: () => void;
  isSubmitting: boolean;
  error: string | null;
}

export function useCreateRepository(): CreateRepositoryState {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createRepository = async (name: string, description: string): Promise<ApiRepo> => {
    setIsSubmitting(true);
    setError(null);
    try {
      const response = await apiClient.post<ApiRepo>('/api/codebases/create', {
        name: name.trim(),
        description: description.trim() || null,
      });
      return response.data;
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
      const message =
        axiosErr.response?.data?.message ||
        axiosErr.message ||
        'Failed to create repository';
      setError(message);
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  };

  const clearError = () => setError(null);

  return { createRepository, clearError, isSubmitting, error };
}
