package org.gitbounty.gitbountybackend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserRepository;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
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

