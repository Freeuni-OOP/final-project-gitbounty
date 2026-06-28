import axios from 'axios';
import type { BountyAPI } from '../types/Bounty';

const API_BASE = '/api/bounties';

// AUTOMATION: Intercept every outgoing HTTP request and attach the security token
axios.interceptors.request.use(
    (config) => {
        // Retrieve the JWT token saved by your Keycloak login flow
        const token = localStorage.getItem('token');

        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export const bountyApi = {
    async getAllBounties(): Promise<BountyAPI[]> {
        const response = await axios.get(`${API_BASE}`);
        return response.data;
    },

    async getBountiesByRepo(repoId: string): Promise<BountyAPI[]> {
        const response = await axios.get(`${API_BASE}/repository/${repoId}`);
        return response.data;
    },

    async getBountyById(id: number): Promise<BountyAPI> {
        const response = await axios.get(`${API_BASE}/${id}`);
        return response.data;
    },
};