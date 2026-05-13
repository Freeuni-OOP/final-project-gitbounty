package org.gitbounty.gitbountybackend.controller;

import java.net.URI;

import org.gitbounty.gitbountybackend.dto.CreateUserRequest;
import org.gitbounty.gitbountybackend.dto.UserResponse;
import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        // The security check happens BEFORE this method is even called
        // thanks to the SecurityConfig rules below.
        User user = userService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(UserResponse.from(user));
    }

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
		String username = requireNonBlank(request.username(), "Username is required");
		String email = requireNonBlank(request.email(), "Email is required");
		String password = requireNonBlank(request.password(), "Password is required");

		User createdUser = userService.createUser(username, email, password);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(createdUser.getId())
			.toUri();

		return ResponseEntity.created(location).body(UserResponse.from(createdUser));
	}

	@ExceptionHandler(DuplicateUserException.class)
	public ResponseEntity<String> handleDuplicateUser(DuplicateUserException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
	}

	private String requireNonBlank(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}

		return value.trim();
	}
}

