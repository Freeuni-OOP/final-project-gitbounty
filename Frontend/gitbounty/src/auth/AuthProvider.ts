export interface AuthProvider {
    initialized: boolean;
    authenticated: boolean;
    token: string | undefined;
    login: () => void;
    logout: () => void;
    getToken: () => Promise<string | undefined>;
}