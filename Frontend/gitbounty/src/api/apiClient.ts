import axios from 'axios';
import {getGlobalAuth} from "../auth/authInstance.ts";
import { API_BASE } from './apiBase';
const apiClient = axios.create({
    baseURL: API_BASE,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor to inject the Authorization header
apiClient.interceptors.request.use(async (config) => {
    const auth = getGlobalAuth();
    if (auth) {
        const token = await auth.getToken();
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default apiClient;