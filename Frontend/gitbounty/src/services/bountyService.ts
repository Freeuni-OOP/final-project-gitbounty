import apiClient from '../api/apiClient';
import type { BountyAPI } from '../types/Bounty';

export const bountyApi = {

    async getAllBounties(): Promise<BountyAPI[]> {
        const response = await apiClient.get('/api/bounties');
        return response.data;
    },

    async getBountiesByRepo(repoId: string): Promise<BountyAPI[]> {
        const response = await apiClient.get(`/api/bounties/repository/${repoId}`);
        return response.data;
    },

    async getBountyById(id: number): Promise<BountyAPI> {
        const response = await apiClient.get(`/api/bounties/${id}`);
        return response.data;
    },
};
