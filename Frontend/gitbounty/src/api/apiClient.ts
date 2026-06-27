import axios from 'axios';
import {authService} from "../components/providers/AuthProvider.tsx";

const apiClient = axios.create({
    baseURL: 'https://api.gitbounty.foo',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor to inject the Authorization header
apiClient.interceptors.request.use(async (config) => {
    const token = await authService.getToken();

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

export default apiClient;