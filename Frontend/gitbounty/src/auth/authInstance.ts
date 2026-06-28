import type { AuthProvider } from "./AuthProvider";

let activeInstance: AuthProvider | null = null;

export const setGlobalAuth = (instance: AuthProvider) => { activeInstance = instance; };
export const getGlobalAuth = () => activeInstance;