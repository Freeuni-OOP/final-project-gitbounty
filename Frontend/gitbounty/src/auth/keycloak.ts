import Keycloak, { type KeycloakConfig } from 'keycloak-js';

// Consider exporting these to global const variables
const keycloakConfig: KeycloakConfig = {
    url: 'https://auth.gitbounty.foo',
    realm: 'gitbounty',
    clientId: 'gitbounty-frontend',
};

const keycloak = new Keycloak(keycloakConfig);
export default keycloak;