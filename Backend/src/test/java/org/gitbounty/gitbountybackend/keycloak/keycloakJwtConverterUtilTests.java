package org.gitbounty.gitbountybackend.keycloak;

import org.gitbounty.gitbountybackend.util.KeycloakJwtConverterUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtConverterUtilTests {

    @Test
    void createConverter_ShouldReturnConfiguredConverter() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("mocked-token-string")
            .header("alg", "none")
            .claim("preferred_username", "alice_dev")
            .claim("scope", "read")
            .build();

        JwtAuthenticationConverter converter = KeycloakJwtConverterUtil.createConverter();

        // Act
        var authentication = converter.convert(jwt);

        // Assert
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("alice_dev");
    }

    @Test
    void convert_WithEmptyClaims_ShouldReturnEmptyAuthorities() {
        // Arrange
        // Add a harmless subject claim so the builder doesn't throw 'claims cannot be empty'
        Jwt jwt = Jwt.withTokenValue("mock")
            .header("alg", "none")
            .claim("sub", "dummy-user")
            .build();

        // Act
        Collection<GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    @Test
    void convert_WithStandardScopes_ShouldConvertWithRolePrefix() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("mock")
            .header("alg", "none")
            .claim("scope", "read write")
            .build();

        // Act
        Collection<GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

        // Assert
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_read", "ROLE_write");
    }

    @Test
    void convert_WithRealmRoles_ShouldExtractAndAppendRolePrefix() {
        // Arrange
        Map<String, Object> realmAccess = Map.of("roles", List.of("admin", "user"));
        Jwt jwt = Jwt.withTokenValue("mock")
            .header("alg", "none")
            .claim("realm_access", realmAccess)
            .build();

        // Act
        Collection<GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

        // Assert
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    void convert_WithResourceClientRoles_ShouldExtractAndAppendRolePrefix() {
        // Arrange
        Map<String, Object> clientAccess = Map.of("roles", List.of("maintainer", "developer"));
        Map<String, Object> resourceAccess = Map.of("gitbounty-backend", clientAccess);
        Jwt jwt = Jwt.withTokenValue("mock")
            .header("alg", "none")
            .claim("resource_access", resourceAccess)
            .build();

        // Act
        Collection<GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

        // Assert
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_maintainer", "ROLE_developer");
    }

    @Test
    void convert_WithCombinedScopesRealmAndClientRoles_ShouldExtractAllWithoutDuplicates() {
        // Arrange
        Map<String, Object> realmAccess = Map.of("roles", List.of("user", "offline_access"));
        Map<String, Object> clientAccess = Map.of("roles", List.of("developer", "user"));
        Map<String, Object> resourceAccess = Map.of("gitbounty-backend", clientAccess);

        Jwt jwt = Jwt.withTokenValue("mock")
            .header("alg", "none")
            .claim("scope", "openid email")
            .claim("realm_access", realmAccess)
            .claim("resource_access", resourceAccess)
            .build();

        // Act
        Collection<GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

        // Assert
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder(
                "ROLE_openid",
                "ROLE_email",
                "ROLE_user",
                "ROLE_offline_access",
                "ROLE_developer"
            );
    }

    @Test
    void convert_WithMalformedOrNullClaims_ShouldHandleGracefullyWithoutCrashing() {
        // Arrange
        Map<String, Object> malformedRealmAccess = new HashMap<>();
        malformedRealmAccess.put("roles", "not-a-collection-object");

        Map<String, Object> malformedResourceAccess = new HashMap<>();
        malformedResourceAccess.put("invalid-client", "string-instead-of-map");

        Jwt jwt = Jwt.withTokenValue("mock")
            .header("alg", "none")
            // Adding a token identifier claim keeps the builder happy while we feed malformed payloads downstream
            .claim("jti", "unique-token-id")
            .claim("realm_access", malformedRealmAccess)
            .claim("resource_access", malformedResourceAccess)
            .build();

        // Act
        Collection<GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    @Test
    void convert_WithNullValuesInCollections_ShouldFilterThemOut() {
        // Arrange
        List<Object> realmRolesWithNull = new ArrayList<>();
        realmRolesWithNull.add("admin");
        realmRolesWithNull.add(null);

        Map<String, Object> realmAccess = Map.of("roles", realmRolesWithNull);
        Jwt jwt = Jwt.withTokenValue("mock")
            .header("alg", "none")
            .claim("realm_access", realmAccess)
            .build();

        // Act
        Collection<GrantedAuthority> authorities = KeycloakJwtConverterUtil.convert(jwt);

        // Assert
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_admin");
    }
}