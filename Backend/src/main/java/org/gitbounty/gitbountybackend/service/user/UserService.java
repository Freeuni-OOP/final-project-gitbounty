package org.gitbounty.gitbountybackend.service.user;

import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public User createUser(String username, String email, String keycloakId) throws DuplicateUserException {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUserException("Username already exists: " + username);
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("Email already exists: " + email);
        }

        return userRepository.save(new User(username, email, keycloakId));
    }
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    /**
     * Synchronizes a Keycloak user token with the local database.
     * Provisions a new user record seamlessly on their first request.
     */
    @Transactional
    public void syncKeycloakUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        // Sub-millisecond lookup thanks to your database index
        if (!userRepository.existsByKeycloakId(keycloakId)) {
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");

            User newUser = new User(username, email, keycloakId);

            try {
                userRepository.save(newUser);
            } catch (Exception e) {
                // Safeguard against concurrent multi-request race conditions
                throw(new DataIntegrityViolationException("couldn't save user"));
            }
        }
    }

    public Optional<User> findByUsername(String ownerUsername) {
        return userRepository.findByUsername(ownerUsername);
    }

    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void delete(User user) {
        userRepository.delete(user);
    }

    /**
     * Updates a user's profile with new username and/or email.
     * Validates that the new username/email don't already exist.
     *
     * @param userId the ID of the user to update
     * @param newUsername the new username (optional, null to keep existing)
     * @param newEmail the new email (optional, null to keep existing)
     * @return the updated User
     * @throws IllegalArgumentException if no user exists for the provided id
     * @throws DuplicateUserException if the requested username or email is already taken
     */
    @Transactional
    public User updateUserProfile(Long userId, String newUsername, String newEmail)
            throws IllegalArgumentException, DuplicateUserException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Validate new username if provided
        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
            if (userRepository.findByUsername(newUsername).isPresent()) {
                throw new DuplicateUserException("Username already exists: " + newUsername);
            }
            user.setUsername(newUsername);
        }

        // Validate new email if provided
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(user.getEmail())) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new DuplicateUserException("Email already exists: " + newEmail);
            }
            user.setEmail(newEmail);
        }

        return userRepository.save(user);
    }
}
