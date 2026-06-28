export interface AuthProvider {
    isLoading: boolean;
    initialized: boolean;
    authenticated: boolean;
    token: string | undefined;
    login: () => void;
    logout: () => void;
    getToken: () => Promise<string | undefined>;
}