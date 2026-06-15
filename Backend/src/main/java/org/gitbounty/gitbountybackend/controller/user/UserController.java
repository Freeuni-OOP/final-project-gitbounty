package org.gitbounty.gitbountybackend.controller.user;

import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

    /**
     * Get user profile by ID
     * @param id the user ID
     * @return UserResponse containing user profile information
     */
    @GetMapping("/{id}")
	public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        // The security check happens BEFORE this method is even called
        // thanks to the SecurityConfig rules below.
        User user = userService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Get current authenticated user's profile
     * @param authentication the current authentication context
     * @return UserResponse containing authenticated user's profile information
     */
    @GetMapping("/profile/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();

        User user = userService.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Update user profile (username and/or email)
     * @param id the user ID to update
     * @param updateRequest the update request containing new username/email
     * @return UserResponse containing updated user profile information
     */
    @PutMapping("/{id}")
    @PreAuthorize("@userPermissions.isOwner(#id, authentication.name)")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest updateRequest) {

        try {
            User updatedUser = userService.updateUserProfile(id, updateRequest.username(), updateRequest.email());
            return ResponseEntity.ok(UserResponse.from(updatedUser));
        } catch (DuplicateUserException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
