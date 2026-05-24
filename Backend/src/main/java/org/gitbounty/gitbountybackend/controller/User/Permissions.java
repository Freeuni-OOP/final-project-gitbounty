package org.gitbounty.gitbountybackend.controller.User;

import org.gitbounty.gitbountybackend.service.User.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Centralized permissions + reusable method-security annotations for the User controller package.
 */
@Component("permissions")
public class Permissions {

    private final UserService userService;

    public Permissions(UserService userService) {
        this.userService = userService;
    }

    public boolean isOwner(Long id, String username) {
        if (id == null || username == null) {
            return false;
        }
        return userService.findById(id)
            .map(user -> username.equals(user.getUsername()))
            .orElse(false);
    }

    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || role == null || role.isBlank()) {
            return false;
        }

        String expectedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> expectedRole.equals(authority.getAuthority()));
    }
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@permissions.isOwner(#id, authentication.name)")
@interface IsOwner {
}