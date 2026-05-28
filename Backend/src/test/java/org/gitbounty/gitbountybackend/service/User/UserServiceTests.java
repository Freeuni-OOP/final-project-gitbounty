package org.gitbounty.gitbountybackend.service.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private String randomUsername() {
        return "svc_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String randomEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@svc.test";
    }

    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Helper to manufacture valid mock JWT tokens matching your filter configurations
     */
    private Jwt createMockJwt(String keycloakId, String username, String email) {
        return Jwt.withTokenValue("mock-token-" + UUID.randomUUID())
            .header("alg", "none")
            .subject(keycloakId)
            .claim("preferred_username", username)
            .claim("email", email)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    @Test
    void syncKeycloakUserProvisionsNewUserOnFirstLogin() {
        // Given
        String keycloakId = randomKeycloakId();
        String username = randomUsername();
        String email = randomEmail();
        Jwt mockJwt = createMockJwt(keycloakId, username, email);

        // When - Simulate the JIT trigger
        userService.syncKeycloakUser(mockJwt);

        // Then - Verify it was written to the DB automatically
        var foundUser = userRepository.findByUsername(username);
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(email);
    }

    @Test
    void syncKeycloakUserIsIdempotentAndDoesNotDuplicateExistingUser() {
        // Given - Pre-seed the user in the database
        String keycloakId = randomKeycloakId();
        String username = randomUsername();
        String email = randomEmail();
        userRepository.save(new User(username, email, keycloakId));

        long initialUserCount = userRepository.count();
        Jwt mockJwt = createMockJwt(keycloakId, username, email);

        // When - User logs in a second time
        userService.syncKeycloakUser(mockJwt);

        // Then - Confirm no duplicate inserts occurred
        assertThat(userRepository.count()).isEqualTo(initialUserCount);
    }

    @Test
    void createUserSucceedsWhenUsernameAndEmailAreUnique() {
        String username = randomUsername();
        String email = randomEmail();
        String keycloakId = randomKeycloakId();
        User created = userService.createUser(username, email, keycloakId);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUsername()).isEqualTo(username);
        assertThat(created.getEmail()).isEqualTo(email);
    }

    @Test
    void createUserFailsGracefullyWhenUsernameAlreadyExists() {
        String username = randomUsername();
        String firstEmail = randomEmail();
        String secondEmail = randomEmail();

        userRepository.save(new User(username, firstEmail, randomKeycloakId()));

        assertThatThrownBy(() -> userService.createUser(username, secondEmail, randomKeycloakId()))
            .isInstanceOf(DuplicateUserException.class)
            .hasMessageContaining("Username already exists");
    }

    @Test
    void createUserFailsGracefullyWhenEmailAlreadyExists() {
        String email = randomEmail();
        String firstUsername = randomUsername();
        String secondUsername = randomUsername();

        userRepository.save(new User(firstUsername, email, randomKeycloakId()));

        assertThatThrownBy(() -> userService.createUser(secondUsername, email, randomKeycloakId()))
            .isInstanceOf(DuplicateUserException.class)
            .hasMessageContaining("Email already exists");
    }

    @Test
    void findByIdReturnsUserWhenExists() {
        String username = randomUsername();
        String email = randomEmail();
        User created = userRepository.save(new User(username, email, randomKeycloakId()));

        var found = userService.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(username);
        assertThat(found.get().getEmail()).isEqualTo(email);
    }
}