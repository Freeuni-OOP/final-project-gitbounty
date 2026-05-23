package org.gitbounty.gitbountybackend.user;

import org.gitbounty.gitbountybackend.controller.User.Permissions;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsTests {

    @Mock
    private UserService userService;

    private Permissions permissions;

    @BeforeEach
    void setUp() {
        permissions = new Permissions(userService);
    }

    @Test
    void isOwner_returnsTrueWhenUsernameMatchesUser() {
        User user = new User("alice", "alice@example.com");
        user.setId(42L);
        when(userService.findById(42L)).thenReturn(Optional.of(user));

        assertThat(permissions.isOwner(42L, "alice")).isTrue();
    }

    @Test
    void isOwner_returnsFalseWhenUsernameDoesNotMatchUser() {
        User user = new User("alice", "alice@example.com");
        user.setId(42L);
        when(userService.findById(42L)).thenReturn(Optional.of(user));

        assertThat(permissions.isOwner(42L, "bob")).isFalse();
    }

    @Test
    void isOwner_returnsFalseWhenUserDoesNotExist() {
        when(userService.findById(42L)).thenReturn(Optional.empty());

        assertThat(permissions.isOwner(42L, "alice")).isFalse();
    }

    @Test
    void hasRole_matchesRoleWithoutPrefix() {
        var authentication = new UsernamePasswordAuthenticationToken(
            "alice",
            "secret",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThat(permissions.hasRole(authentication, "ADMIN")).isTrue();
    }

    @Test
    void hasRole_acceptsPrefixedRole() {
        var authentication = new UsernamePasswordAuthenticationToken(
            "alice",
            "secret",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThat(permissions.hasRole(authentication, "ROLE_ADMIN")).isTrue();
    }

    @Test
    void hasRole_returnsFalseForMissingRole() {
        var authentication = new UsernamePasswordAuthenticationToken(
            "alice",
            "secret",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThat(permissions.hasRole(authentication, "ADMIN")).isFalse();
    }
}

