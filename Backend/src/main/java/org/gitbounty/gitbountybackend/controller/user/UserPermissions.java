package org.gitbounty.gitbountybackend.controller.user;

import org.gitbounty.gitbountybackend.service.User.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Centralized permissions methods for method-security annotations.
 */
@Component("userPermissions")
public class UserPermissions {

    private final UserService userService;

    public UserPermissions(UserService userService) {
        this.userService = userService;
    }

    /**
     * Check whether the authenticated username matches the target user's username.
     */
    public boolean isOwner(Long targetUserId, String authenticatedUsername) {
        if (targetUserId == null || authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return false;
        }
        return userService.findById(targetUserId)
            .map(user -> authenticatedUsername.equals(user.getUsername()))
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
