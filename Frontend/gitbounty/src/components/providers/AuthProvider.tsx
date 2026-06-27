import React, { createContext, useContext } from 'react';
import type {AuthProvider} from "../../auth/AuthProvider.ts";
import {KeycloakAdapter} from "../../auth/KeycloakAdapter.ts";

const authService = new KeycloakAdapter();
const AuthContext = createContext<AuthProvider>(authService);

export const AuthContextProvider = ({ children }: { children: React.ReactNode }) => {
    return <AuthContext.Provider value={authService}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);