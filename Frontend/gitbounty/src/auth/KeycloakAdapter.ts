import keycloak from './keycloak';
import type { AuthProvider } from "./AuthProvider.ts";

export class KeycloakAdapter implements AuthProvider {
    private readonly onUpdate: () => void;
    private initializedStatus = false;
    private initPromise: Promise<void> | null = null;

    get isLoading() { return !this.initializedStatus; }
    get initialized() { return this.initializedStatus; }
    get authenticated() { return keycloak.authenticated; }
    get token() { return keycloak.token || undefined; }

    constructor(onUpdate: () => void) {
        this.onUpdate = onUpdate;
        keycloak.onAuthSuccess = () => this.onUpdate();
        keycloak.onAuthLogout = () => this.onUpdate();
        keycloak.onTokenExpired = () => {
            keycloak.updateToken(30)
                .then(() => this.onUpdate())
                .catch((err) => {
                    console.error("Token refresh failed, logging out", err);
                    this.onUpdate();
                });
        };
    }

    public async initialize(): Promise<void> {
        if (this.initPromise) return this.initPromise;
        this.initPromise = (async () => {
            try {
                await keycloak.init({
                    onLoad: 'check-sso',
                    silentCheckSsoRedirectUri: globalThis.location.origin + '/silent-check-sso.html',
                    pkceMethod: 'S256',
                });
            } catch (e) {
                console.error("Keycloak init failed", e);
            } finally {
                this.initializedStatus = true;
                this.onUpdate();
            }
        })();
        return this.initPromise;
    }

    login() { keycloak.login(); }
    logout() { keycloak.logout(); }

    async getToken() {
        try {
            await keycloak.updateToken(30);
            return keycloak.token || undefined;
        } catch (e) {
            // Token refresh failed, likely due to session expiry
            console.warn("Token refresh failed");
            this.onUpdate(); // Tell React to re-render and see 'authenticated: false'
            return undefined;
        }
    }
}