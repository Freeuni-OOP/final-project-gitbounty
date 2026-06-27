import Keycloak, { type KeycloakConfig } from 'keycloak-js';

// Consider exporting these to global const variables
const keycloakConfig: KeycloakConfig = {
    url: 'https://auth.gitbounty.foo',
    realm: 'gitbounty',
    clientId: 'gitbounty-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

// Keep the promise persistent to ensure single initialization
let initPromise: Promise<boolean> | null = null;

/**
 * Initializes Keycloak and returns a promise.
 */
export const initializeKeycloak = (): Promise<boolean> => {
    if (initPromise) return initPromise;

    initPromise = keycloak
        .init({
            onLoad: 'check-sso',
            silentCheckSsoRedirectUri: globalThis.location.origin + '/silent-check-sso.html',
            pkceMethod: 'S256', // Always use PKCE for public clients
        })
        .then((authenticated) => {
            console.info(authenticated ? 'User is authenticated' : 'User is a guest');
            return authenticated;
        })
        .catch((error) => {
            console.error('Keycloak initialization failed:', error);
            throw error; // Let the caller handle the failure
        });

    return initPromise;
};
export default keycloak;