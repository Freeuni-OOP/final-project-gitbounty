import React, {createContext, useContext, useEffect, useMemo, useState} from 'react';
import type {AuthProvider} from "../../auth/AuthProvider.ts";
import {KeycloakAdapter} from "../../auth/KeycloakAdapter.ts";

// Create the Context with a default null value
// (We handle the initialization check inside the Provider)
const AuthContext = createContext<AuthProvider | null>(null);

export const AuthContextProvider = ({ children }: { children: React.ReactNode }) => {
    // 1. A state to force re-render
    const [, setTick] = useState(0);
    const forceUpdate = () => setTick(t => t + 1);

    // 2. Memoize the instance
    const authService = useMemo(() => new KeycloakAdapter(forceUpdate), []);

    // 3. Initialize
    useEffect(() => {
        authService.initialize();
    }, [authService]);

    // 4. Create a fresh value object on every render
    // This allows components to see the latest values from authService
    const value = useMemo(() => ({
        isLoading: authService.isLoading,
        initialized: authService.initialized,
        authenticated: authService.authenticated,
        token: authService.token,
        login: () => authService.login(),
        logout: () => authService.logout(),
        getToken: () => authService.getToken(),
    }), [
        authService.isLoading,
        authService.initialized,
        authService.authenticated,
        authService.token
    ]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        return {
            isLoading: true,
            initialized: false,
            authenticated: false,
            token: undefined,
            login: () => {},
            logout: () => {},
            getToken: async () => undefined
        };
    }
    return context;
};