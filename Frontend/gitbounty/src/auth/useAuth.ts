import { useSyncExternalStore } from "react";
import { getGlobalAuth, subscribeAuth } from "./authInstance";

interface AuthSnapshot {
    isLoading: boolean;
    initialized: boolean;
    authenticated: boolean;
    token: string | undefined;
}

// Cache the last snapshot so we don't create a new object on every
// render unless something actually changed.
let lastSnapshot: AuthSnapshot | null = null;

function getSnapshot(): AuthSnapshot {
    const auth = getGlobalAuth();

    const next: AuthSnapshot = auth
        ? {
            isLoading: auth.isLoading,
            initialized: auth.initialized,
            authenticated: auth.authenticated,
            token: auth.token,
        }
        : {
            isLoading: true,
            initialized: false,
            authenticated: false,
            token: undefined,
        };

    if (
        lastSnapshot &&
        lastSnapshot.isLoading === next.isLoading &&
        lastSnapshot.initialized === next.initialized &&
        lastSnapshot.authenticated === next.authenticated &&
        lastSnapshot.token === next.token
    ) {
        return lastSnapshot;
    }

    lastSnapshot = next;
    return next;
}

export function useAuth(): AuthSnapshot {
    return useSyncExternalStore(subscribeAuth, getSnapshot);
}