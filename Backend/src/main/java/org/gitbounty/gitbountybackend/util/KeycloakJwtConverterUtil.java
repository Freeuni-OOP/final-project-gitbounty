package org.gitbounty.gitbountybackend.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Static helper for converting Keycloak JWT claims into Spring authorities.
 * Converts:
 * - standard granted authorities (scopes)
 * - Keycloak realm roles at claim `realm_access.roles`
 * - Keycloak client/resource roles at claim `resource_access.<client>.roles`
 */
public final class KeycloakJwtConverterUtil {

    private static final String ROLE_PREFIX = "ROLE_";

    private KeycloakJwtConverterUtil() {
        // utility class
    }

    /**
     * Create a JwtAuthenticationConverter configured for Keycloak tokens.
     */
    public static org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter createConverter() {
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(KeycloakJwtConverterUtil::convert);
        return converter;
    }

    /**
     * Convert a JWT into a collection of Spring GrantedAuthority.
     */
    public static Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addDefaultConvertedAuthorities(jwt, authorities);

        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof java.util.Collection<?> roles) {
            addRolesFromCollection(roles, authorities);
        }

        java.util.Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().stream()
                .filter(v -> v instanceof java.util.Map<?, ?>)
                .map(v -> (java.util.Map<?, ?>) v)
                .map(m -> m.get("roles"))
                .filter(v -> v instanceof java.util.Collection<?>)
                .map(v -> (java.util.Collection<?>) v)
                .flatMap(java.util.Collection::stream)
                .filter(Objects::nonNull)
                .forEach(r -> addRolesFromCollection(java.util.Collections.singletonList(r), authorities));
        }

        return authorities;
    }

    private static JwtGrantedAuthoritiesConverter createDefaultConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        defaultConverter.setAuthorityPrefix(ROLE_PREFIX);
        return defaultConverter;
    }

    private static void addDefaultConvertedAuthorities(Jwt jwt, Set<GrantedAuthority> authorities) {
        var defaultConverter = createDefaultConverter();
        var defaultAuths = defaultConverter.convert(jwt);
        defaultAuths.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);
    }

    private static void addRolesFromCollection(Collection<?> roles, Set<GrantedAuthority> authorities) {
        if (roles == null) {
            return;
        }

        roles.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .map(r -> new SimpleGrantedAuthority(ROLE_PREFIX + r))
            .forEach(authorities::add);
    }
}

