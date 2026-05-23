package org.gitbounty.gitbountybackend.apis;

import org.gitbounty.gitbountybackend.apis.dto.KeycloakOpenIdConfigurationResponse;
import org.gitbounty.gitbountybackend.apis.dto.KeycloakTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Handles HTTP calls to Keycloak (OIDC discovery and token exchange).
 * Uses RestClient.Builder for testability and centralized client configuration.
 */
@Component
public class KeycloakApi {

    private final RestClient restClient;
    private final String issuerUri;
    private final String clientId;
    private final String clientSecret;

    // cached token endpoint once discovered
    private volatile String tokenEndpoint;

    public KeycloakApi(RestClient.Builder restClientBuilder,
                       @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                       @Value("${keycloak.client-id:}") String clientId,
                       @Value("${keycloak.client-secret:}") String clientSecret) {
        this.restClient = restClientBuilder.build();
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String requestAccessToken(String username, String password) {
        String resolvedTokenEndpoint = getTokenEndpoint();
        MultiValueMap<String, String> form = buildPasswordGrantRequest(username, password);

        try {
            KeycloakTokenResponse tokenResponse = restClient.post()
                    .uri(resolvedTokenEndpoint)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (tokenResponse == null) {
                throw new BadCredentialsException("Authentication failed");
            }

            String accessToken = tokenResponse.accessToken();
            if (accessToken == null || accessToken.isBlank()) {
                throw new BadCredentialsException("No access_token returned from identity provider");
            }

            return accessToken;
        } catch (RestClientResponseException e) {
            throw new BadCredentialsException("Authentication failed at identity provider: " + e.getStatusCode());
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication request failed: " + e.getMessage());
        }
    }

    private MultiValueMap<String, String> buildPasswordGrantRequest(String username,
                                                                    String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);

        if (clientId != null && !clientId.isBlank()) {
            form.add("client_id", clientId);
        }
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        return form;
    }

    private String getTokenEndpoint() {
        if (tokenEndpoint != null) {
            return tokenEndpoint;
        }

        synchronized (this) {
            if (tokenEndpoint != null) {
                return tokenEndpoint;
            }

            String wellKnown = issuerUri;
            if (!wellKnown.endsWith("/")) {
                wellKnown = wellKnown + "/";
            }
            wellKnown = wellKnown + ".well-known/openid-configuration";

            try {
                KeycloakOpenIdConfigurationResponse config = restClient.get()
                        .uri(wellKnown)
                        .retrieve()
                        .body(KeycloakOpenIdConfigurationResponse.class);

                if (config == null) {
                    throw new IllegalStateException("Discovery document empty");
                }

                String discoveredTokenEndpoint = config.tokenEndpoint();
                if (discoveredTokenEndpoint == null || discoveredTokenEndpoint.isBlank()) {
                    throw new IllegalStateException("token_endpoint not present in discovery document");
                }

                tokenEndpoint = discoveredTokenEndpoint;
                return tokenEndpoint;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve token endpoint from issuer discovery: " + e.getMessage(), e);
            }
        }
    }
}
