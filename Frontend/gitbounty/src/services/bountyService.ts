import apiClient from '../api/apiClient';
import type {
    BountyAPI,
    CreateBountyRequest,
} from '../types/Bounty';

export const bountyApi = {
    async getAllBounties(): Promise<BountyAPI[]> {
        const response = await apiClient.get<BountyAPI[]>(
            '/api/bounties'
        );

        return response.data;
    },

    async getBountiesByRepo(
        repoId: string
    ): Promise<BountyAPI[]> {
        const response = await apiClient.get<BountyAPI[]>(
            `/api/bounties/repository/${repoId}`
        );

        return response.data;
    },

    async getBountyById(
        id: number
    ): Promise<BountyAPI> {
        const response = await apiClient.get<BountyAPI>(
            `/api/bounties/${id}`
        );

        return response.data;
    },

    async getMyRepositoryBounties(): Promise<BountyAPI[]> {
        const response = await apiClient.get<BountyAPI[]>(
            '/api/bounties/repository/me'
        );

        return response.data;
    },

    async getMyClaimedBounties(): Promise<BountyAPI[]> {
        const response = await apiClient.get<BountyAPI[]>(
            '/api/bounties/claimed/me'
        );

        return response.data;
    },

    async createBounty(
        request: CreateBountyRequest
    ): Promise<BountyAPI> {
        const response = await apiClient.post<BountyAPI>(
            '/api/bounties',
            request
        );

        return response.data;
    },

    async cancelBounty(id: number): Promise<void> {
        await apiClient.post(
            `/api/bounties/${id}/cancel`
        );
    },

    async claimBounty(id: number): Promise<BountyAPI> {
        const response = await apiClient.post<BountyAPI>(
            `/api/bounties/${id}/claim`
        );

        return response.data;
    },

    async unclaimBounty(id: number): Promise<BountyAPI> {
        const response = await apiClient.post<BountyAPI>(
            `/api/bounties/${id}/unclaim`
        );

        return response.data;
    },
};