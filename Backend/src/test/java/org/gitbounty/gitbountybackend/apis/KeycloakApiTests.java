package org.gitbounty.gitbountybackend.apis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakApiTests {

    private static final String ISSUER = "http://localhost:8080/realms/gitbounty";
    private static final String WELL_KNOWN = ISSUER + "/.well-known/openid-configuration";
    private static final String TOKEN_ENDPOINT = ISSUER + "/protocol/openid-connect/token";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void requestAccessToken_success() {
        KeycloakApi keycloakApi = new KeycloakApi(restClientBuilder, ISSUER, "backend-client", "super-secret");

        server.expect(ExpectedCount.once(), requestTo(WELL_KNOWN))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"token_endpoint\":\"" + TOKEN_ENDPOINT + "\"}", MediaType.APPLICATION_JSON));

        server.expect(ExpectedCount.once(), requestTo(TOKEN_ENDPOINT))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("grant_type=password")))
            .andExpect(content().string(containsString("username=alice")))
            .andExpect(content().string(containsString("password=secret")))
            .andExpect(content().string(containsString("client_id=backend-client")))
            .andExpect(content().string(containsString("client_secret=super-secret")))
            .andRespond(withSuccess("{\"access_token\":\"token-value\"}", MediaType.APPLICATION_JSON));

        String token = keycloakApi.requestAccessToken("alice", "secret");

        assertThat(token).isEqualTo("token-value");
        server.verify();
    }

    @Test
    void requestAccessToken_cachesDiscoveredTokenEndpoint() {
        KeycloakApi keycloakApi = new KeycloakApi(restClientBuilder, ISSUER, "backend-client", "super-secret");

        server.expect(ExpectedCount.once(), requestTo(WELL_KNOWN))
            .andRespond(withSuccess("{\"token_endpoint\":\"" + TOKEN_ENDPOINT + "\"}", MediaType.APPLICATION_JSON));

        server.expect(ExpectedCount.twice(), requestTo(TOKEN_ENDPOINT))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"access_token\":\"token-value\"}", MediaType.APPLICATION_JSON));

        keycloakApi.requestAccessToken("alice", "secret");
        keycloakApi.requestAccessToken("alice", "secret");

        server.verify();
    }

    @Test
    void requestAccessToken_throwsBadCredentialsOnUnauthorized() {
        KeycloakApi keycloakApi = new KeycloakApi(restClientBuilder, ISSUER, "backend-client", "super-secret");

        server.expect(requestTo(WELL_KNOWN))
            .andRespond(withSuccess("{\"token_endpoint\":\"" + TOKEN_ENDPOINT + "\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(TOKEN_ENDPOINT))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> keycloakApi.requestAccessToken("alice", "wrong"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Authentication failed at identity provider");
    }

    @Test
    void requestAccessToken_omitsBlankClientCredentials() {
        KeycloakApi keycloakApi = new KeycloakApi(restClientBuilder, ISSUER, " ", "");

        server.expect(requestTo(WELL_KNOWN))
            .andRespond(withSuccess("{\"token_endpoint\":\"" + TOKEN_ENDPOINT + "\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(TOKEN_ENDPOINT))
            .andExpect(content().string(not(containsString("client_id="))))
            .andExpect(content().string(not(containsString("client_secret="))))
            .andRespond(withSuccess("{\"access_token\":\"token-value\"}", MediaType.APPLICATION_JSON));

        String token = keycloakApi.requestAccessToken("alice", "secret");
        assertThat(token).isEqualTo("token-value");
    }

    @Test
    void requestAccessToken_failsWhenDiscoveryResponseMissesTokenEndpoint() {
        KeycloakApi keycloakApi = new KeycloakApi(restClientBuilder, ISSUER, "backend-client", "super-secret");

        server.expect(requestTo(WELL_KNOWN))
            .andRespond(withSuccess("{\"issuer\":\"" + ISSUER + "\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> keycloakApi.requestAccessToken("alice", "secret"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to resolve token endpoint from issuer discovery")
            .hasMessageContaining("token_endpoint not present in discovery document");
    }
}

