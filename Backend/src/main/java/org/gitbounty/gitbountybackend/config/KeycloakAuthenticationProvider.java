package org.gitbounty.gitbountybackend.config;

import org.gitbounty.gitbountybackend.apis.KeycloakApi;
import org.gitbounty.gitbountybackend.util.KeycloakJwtConverterUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * AuthenticationProvider that validates HTTP Basic username/password by delegating
 * Keycloak HTTP requests to KeycloakApi, then converting the resulting JWT into Spring auth.
 */
@Component
public class KeycloakAuthenticationProvider implements AuthenticationProvider {

    private final JwtDecoder jwtDecoder;
    private final KeycloakApi keycloakApi;

    public KeycloakAuthenticationProvider(JwtDecoder jwtDecoder, KeycloakApi keycloakApi) {
        this.jwtDecoder = jwtDecoder;
        this.keycloakApi = keycloakApi;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = (authentication.getPrincipal() == null) ? "" : authentication.getName();
        String password = (authentication.getCredentials() == null) ? "" : authentication.getCredentials().toString();

        if (username.isBlank() || password.isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = keycloakApi.requestAccessToken(username, password);
        return registerSpringAuthUser(accessToken);
    }

    private Authentication registerSpringAuthUser(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            Collection<? extends GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

            String principal = jwt.getClaimAsString("preferred_username");
            if (principal == null) {
                principal = jwt.getSubject();
            }

            UsernamePasswordAuthenticationToken result =
                new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
            result.setDetails(Map.of("access_token", accessToken));
            return result;
        } catch (JwtException je) {
            throw new BadCredentialsException("Failed to decode token: " + je.getMessage(), je);
        }
    }

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

