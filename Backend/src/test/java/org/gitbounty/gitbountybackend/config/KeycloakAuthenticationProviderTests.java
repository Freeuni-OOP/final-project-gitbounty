package org.gitbounty.gitbountybackend.config;

import org.gitbounty.gitbountybackend.apis.KeycloakApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthenticationProviderTests {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private KeycloakApi keycloakApi;

    private KeycloakAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new KeycloakAuthenticationProvider(jwtDecoder, keycloakApi);
    }

    @Test
    void authenticate_success() {
        String accessToken = "access.token.value";
        when(keycloakApi.requestAccessToken("alice", "secret")).thenReturn(accessToken);
        when(jwtDecoder.decode(accessToken)).thenReturn(jwt());

        Authentication result =
            provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret"));

        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo("alice");
        assertThat(result.getCredentials()).isEqualTo(accessToken);
        assertThat(result.getAuthorities()).isNotEmpty();
        verify(keycloakApi).requestAccessToken("alice", "secret");
        verify(jwtDecoder).decode(accessToken);
    }

    @Test
    void authenticate_usesSubjectWhenPreferredUsernameMissing() {
        String accessToken = "access.token.value";
        when(keycloakApi.requestAccessToken("alice", "secret")).thenReturn(accessToken);
        when(jwtDecoder.decode(accessToken)).thenReturn(jwtWithoutPreferredUsername());

        Authentication result =
            provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret"));

        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isEqualTo("sub-only");
    }

    @Test
    void authenticate_rejectsBlankUsername() {
        assertThatThrownBy(() ->
            provider.authenticate(new UsernamePasswordAuthenticationToken("   ", "secret")))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid credentials");
    }

    @Test
    void authenticate_rejectsBlankPassword() {
        assertThatThrownBy(() ->
            provider.authenticate(new UsernamePasswordAuthenticationToken("alice", " ")))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid credentials");
    }

    @Test
    void authenticate_wrapsJwtDecodeErrors() {
        String accessToken = "access.token.value";
        when(keycloakApi.requestAccessToken("alice", "secret")).thenReturn(accessToken);
        when(jwtDecoder.decode(accessToken)).thenThrow(new JwtException("bad token"));

        assertThatThrownBy(() ->
            provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret")))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Failed to decode token");
    }

    @Test
    void supports_usernamePasswordToken() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    }

    @Test
    void supports_otherTypes() {
        assertThat(provider.supports(String.class)).isFalse();
    }

    private Jwt jwt() {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            Map.of(
                "preferred_username", "alice",
                "sub", "alice-sub",
                "realm_access", Map.of("roles", java.util.List.of("user"))
            )
        );
    }

    private Jwt jwtWithoutPreferredUsername() {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            Map.of(
                "sub", "sub-only",
                "realm_access", Map.of("roles", java.util.List.of("user"))
            )
        );
    }
}
