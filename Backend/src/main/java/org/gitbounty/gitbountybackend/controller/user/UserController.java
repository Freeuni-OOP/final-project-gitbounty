package org.gitbounty.gitbountybackend.controller.user;

import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserPermissions userPermissions;

    public UserController(UserService userService, UserPermissions userPermissions) {
        this.userService = userService;
        this.userPermissions = userPermissions;
    }

    /**
     * Get user profile by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Get current authenticated user's profile
     */
    @GetMapping("/profile/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(
        @AuthenticationPrincipal Jwt jwt
    ) {

        User user = userService.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new UserNotFoundException("User profile not found"));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Update user profile (username and/or email)
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest updateRequest,
            @AuthenticationPrincipal Jwt jwt) {

        if(!userPermissions.isOwnerById(id, jwt.getSubject())) {
            throw new AccessDeniedException("Access denied");
        }
        // Clean and explicit. Exceptions propagate to the GlobalExceptionHandler.
        User updatedUser = userService.updateUserProfile(id, updateRequest.username(), updateRequest.email());
        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }
}