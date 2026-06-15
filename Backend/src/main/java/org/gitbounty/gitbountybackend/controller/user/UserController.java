package org.gitbounty.gitbountybackend.controller.user;

import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User profile not found for username: " + username));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Update user profile (username and/or email)
     */
    @PutMapping("/{id}")
    @PreAuthorize("@userPermissions.isOwner(#id, authentication.name)")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest updateRequest) {

        // Clean and explicit. Exceptions propagate to the GlobalExceptionHandler.
        User updatedUser = userService.updateUserProfile(id, updateRequest.username(), updateRequest.email());
        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }
}