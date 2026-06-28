import type { AuthProvider } from "./AuthProvider";

let activeInstance: AuthProvider | null = null;
const listeners = new Set<() => void>();

export const setGlobalAuth = (instance: AuthProvider) => {
    activeInstance = instance;
    notifyAuthChanged();
};

export const getGlobalAuth = () => activeInstance;

// Call this any time a field on the active auth instance changes:
// isLoading flips, token refreshes, user logs in/out, etc.
export const notifyAuthChanged = () => {
    listeners.forEach((callback) => callback());
};

// Used internally by useAuth() — don't call this directly elsewhere
export const subscribeAuth = (callback: () => void) => {
    listeners.add(callback);
    return () => listeners.delete(callback);
};