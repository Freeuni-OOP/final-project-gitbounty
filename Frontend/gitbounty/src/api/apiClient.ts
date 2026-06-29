import axios from 'axios';
import {getGlobalAuth} from "../auth/authInstance.ts";

const apiClient = axios.create({
    baseURL: import.meta.env.PROD ? 'https://api.gitbounty.foo' : '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor to inject the Authorization header
apiClient.interceptors.request.use(async (config) => {
    const auth = getGlobalAuth();
    if (auth) {
        const token = await auth.getToken();
        // only when the user is actually logged in, prevents sending "Bearer undefined/null" for public endpoints
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
    }
    return config;
});

export default apiClient;