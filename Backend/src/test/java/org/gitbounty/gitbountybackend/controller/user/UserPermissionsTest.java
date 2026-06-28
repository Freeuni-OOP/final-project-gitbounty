package org.gitbounty.gitbountybackend.controller.user;

import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPermissionsTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserPermissions userPermissions;

    private static User testUser(Long id, String username, String keycloakId) {
        User user = new User(username, username + "@test.com", keycloakId);
        user.setId(id);
        return user;
    }

    @Test
    void isOwner_ShouldReturnTrue_WhenUsernameMatches() {
        User user = testUser(1L, "demo", "kc-demo");

        when(userService.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userPermissions.isOwner(1L, "demo")).isTrue();

        verify(userService).findById(1L);
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        User user = testUser(1L, "demo", "kc-demo");

        when(userService.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userPermissions.isOwner(1L, "other")).isFalse();
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenUserMissing() {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        assertThat(userPermissions.isOwner(99L, "demo")).isFalse();
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenInputInvalid() {
        assertThat(userPermissions.isOwner(null, "demo")).isFalse();
        assertThat(userPermissions.isOwner(1L, null)).isFalse();
        assertThat(userPermissions.isOwner(1L, "   ")).isFalse();

        verifyNoInteractions(userService);
    }

    @Test
    void isOwnerById_ShouldReturnTrue_WhenKeycloakUserMatchesTargetId() {
        User user = testUser(1L, "demo", "kc-demo");

        when(userService.findByKeycloakId("kc-demo")).thenReturn(Optional.of(user));

        assertThat(userPermissions.isOwnerById(1L, "kc-demo")).isTrue();

        verify(userService).findByKeycloakId("kc-demo");
    }

    @Test
    void isOwnerById_ShouldReturnFalse_WhenKeycloakUserHasDifferentId() {
        User user = testUser(2L, "demo", "kc-demo");

        when(userService.findByKeycloakId("kc-demo")).thenReturn(Optional.of(user));

        assertThat(userPermissions.isOwnerById(1L, "kc-demo")).isFalse();
    }

    @Test
    void isOwnerById_ShouldReturnFalse_WhenUserMissing() {
        when(userService.findByKeycloakId("ghost")).thenReturn(Optional.empty());

        assertThat(userPermissions.isOwnerById(1L, "ghost")).isFalse();
    }

    @Test
    void isOwnerById_ShouldReturnFalse_WhenInputInvalid() {
        assertThat(userPermissions.isOwnerById(null, "kc-demo")).isFalse();
        assertThat(userPermissions.isOwnerById(1L, null)).isFalse();
        assertThat(userPermissions.isOwnerById(1L, "   ")).isFalse();

        verifyNoInteractions(userService);
    }

    @Test
    void hasRole_ShouldReturnTrue_WhenAuthenticationHasRoleWithoutPrefixInput() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "demo",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThat(userPermissions.hasRole(authentication, "ADMIN")).isTrue();
    }

    @Test
    void hasRole_ShouldReturnTrue_WhenAuthenticationHasRoleWithPrefixInput() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "demo",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThat(userPermissions.hasRole(authentication, "ROLE_ADMIN")).isTrue();
    }

    @Test
    void hasRole_ShouldReturnFalse_WhenAuthenticationDoesNotHaveRole() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "demo",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThat(userPermissions.hasRole(authentication, "ADMIN")).isFalse();
    }

    @Test
    void hasRole_ShouldReturnFalse_WhenInputInvalid() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "demo",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThat(userPermissions.hasRole(null, "ADMIN")).isFalse();
        assertThat(userPermissions.hasRole(authentication, null)).isFalse();
        assertThat(userPermissions.hasRole(authentication, "   ")).isFalse();
    }
}