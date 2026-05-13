package org.gitbounty.gitbountybackend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.gitbounty.gitbountybackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        User created = userService.createUser(username, email);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUsername()).isEqualTo(username);
        assertThat(created.getEmail()).isEqualTo(email);
    }

    @Test
    void createUserFailsGracefullyWhenUsernameAlreadyExists() {
        String username = randomUsername();
        String firstEmail = randomEmail();
        String secondEmail = randomEmail();

        userRepository.save(new User(username, firstEmail));

        assertThatThrownBy(() -> userService.createUser(username, secondEmail))
            .isInstanceOf(DuplicateUserException.class)
            .hasMessageContaining("Username already exists");
    }

    @Test
    void createUserFailsGracefullyWhenEmailAlreadyExists() {
        String email = randomEmail();
        String firstUsername = randomUsername();
        String secondUsername = randomUsername();

        userRepository.save(new User(firstUsername, email));

        assertThatThrownBy(() -> userService.createUser(secondUsername, email))
            .isInstanceOf(DuplicateUserException.class)
            .hasMessageContaining("Email already exists");
    }

    @Test
    void createUserHashesPasswordBeforePersisting() {
        String username = randomUsername();
        String email = randomEmail();
        String rawPassword = "secret-password";

        User created = userService.createUser(username, email, rawPassword);

        assertThat(created.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, created.getPasswordHash())).isTrue();
    }
    @Test
    void findByIdReturnsUserWhenExists() {
        String username = randomUsername();
        String email = randomEmail();
        User created = userRepository.save(new User(username, email));

        var found = userService.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(username);
        assertThat(found.get().getEmail()).isEqualTo(email);
    }
}

