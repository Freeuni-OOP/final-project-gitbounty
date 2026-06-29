import React, { createContext, useContext, useMemo } from 'react';
import type { AuthProvider } from "../../auth/AuthProvider.ts";
import { useAuth as useExternalAuth } from "../../auth/useAuth.ts";
import { getGlobalAuth } from "../../auth/authInstance.ts";

const AuthContext = createContext<AuthProvider | null>(null);

export const AuthContextProvider = ({ children }: { children: React.ReactNode }) => {
    const authSnapshot = useExternalAuth();

    const contextValue = useMemo<AuthProvider>(() => {
        const globalAuth = getGlobalAuth();
        return {
            isLoading: authSnapshot.isLoading,
            initialized: authSnapshot.initialized,
            authenticated: authSnapshot.authenticated,
            token: authSnapshot.token,
            login: () => globalAuth?.login(),
            logout: () => globalAuth?.logout(),
            getToken: () => globalAuth?.getToken() ?? Promise.resolve(undefined),
        };
    }, [authSnapshot]);

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