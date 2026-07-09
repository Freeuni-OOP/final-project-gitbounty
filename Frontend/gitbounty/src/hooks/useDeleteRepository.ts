import { useState } from 'react';
import apiClient from '../api/apiClient';

interface DeleteRepositoryState {
  deleteRepository: (repositoryId: number) => Promise<void>;
  clearError: () => void;
  isSubmitting: boolean;
  error: string | null;
}

export function useDeleteRepository(): DeleteRepositoryState {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const deleteRepository = async (repositoryId: number): Promise<void> => {
    setIsSubmitting(true);
    setError(null);
    try {
      await apiClient.delete(`/api/codebases/${repositoryId}`);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
      const message =
        axiosErr.response?.data?.message ||
        axiosErr.message ||
        'Failed to delete repository';
      setError(message);
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  };

  const clearError = () => setError(null);

  return { deleteRepository, clearError, isSubmitting, error };
}
