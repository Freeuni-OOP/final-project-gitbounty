import apiClient from '../api/apiClient';
import type { BountyAPI } from '../types/Bounty';

export const bountyApi = {

    async getAllBounties(): Promise<BountyAPI[]> {
        const response = await apiClient.get('/bounties');
        return response.data;
    },

    async getBountiesByRepo(repoId: string): Promise<BountyAPI[]> {
        const response = await apiClient.get(`/bounties/repository/${repoId}`);
        return response.data;
    },

    async getBountyById(id: number): Promise<BountyAPI> {
        const response = await apiClient.get(`/bounties/${id}`);
        return response.data;
    },
};
