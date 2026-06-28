import React, {createContext, useContext, useEffect, useMemo, useState} from 'react';
import type {AuthProvider} from "../../auth/AuthProvider.ts";
import {KeycloakAdapter} from "../../auth/KeycloakAdapter.ts";
import {setGlobalAuth} from "../../auth/authInstance.ts";

const AuthContext = createContext<AuthProvider | null>(null);

export const AuthContextProvider = ({ children }: { children: React.ReactNode }) => {
    const [tick, setTick] = useState(0);

    const authService = useMemo(() => {
        const adapter = new KeycloakAdapter(() => setTick(t => t + 1));
        setGlobalAuth(adapter);
        return adapter;
    }, []);

    useEffect(() => {
        authService.initialize();
    }, [authService]);

    const contextValue = useMemo<AuthProvider>(() => ({
        isLoading: authService.isLoading,
        initialized: authService.initialized,
        authenticated: authService.authenticated,
        token: authService.token,
        login: () => authService.login(),
        logout: () => authService.logout(),
        getToken: () => authService.getToken(),
    }), [tick, authService]);

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    );
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