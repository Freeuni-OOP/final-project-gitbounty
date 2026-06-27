import axios from 'axios';
import {getGlobalAuth} from "../auth/authInstance.ts";
const apiClient = axios.create({
    baseURL: 'https://api.gitbounty.foo',
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