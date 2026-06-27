import keycloak from './keycloak';
import type {AuthProvider} from './AuthProvider';

export class KeycloakAdapter implements AuthProvider {
    get initialized() { return !!keycloak.token; } // Simplification
    get authenticated() { return keycloak.authenticated; }
    get token() { return keycloak.token; }

    login() { keycloak.login(); }
    logout() { keycloak.logout(); }

    async getToken() {
        await keycloak.updateToken(30);
        return keycloak.token;
    }
}