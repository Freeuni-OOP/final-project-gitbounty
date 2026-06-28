import Keycloak, { type KeycloakConfig } from 'keycloak-js';

// Consider exporting these to global const variables
const keycloakConfig: KeycloakConfig = {
    url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8080',
    realm: 'gitbounty',
    clientId: 'gitbounty-frontend',
};

const keycloak = new Keycloak(keycloakConfig);
export default keycloak;