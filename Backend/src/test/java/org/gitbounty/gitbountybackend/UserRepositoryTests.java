package org.gitbounty.gitbountybackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import java.time.LocalDateTime;

import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    private String generateRandomUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateRandomEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    @Test
    void canSaveAndRetrieveUser() {
        String username = generateRandomUsername();
        String email = generateRandomEmail();
        User user = new User(username, email);
        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo(username);
        assertThat(savedUser.getEmail()).isEqualTo(email);
    }

    @Test
    void canFindUserByUsername() {
        String username = generateRandomUsername();
        String email = generateRandomEmail();
        User user = new User(username, email);
        userRepository.save(user);

        var found = userRepository.findByUsername(username);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(email);
    }

    @Test
    void canCreateUserWithRandomData() {
        String randomUsername = generateRandomUsername();
        String randomEmail = generateRandomEmail();

        User newUser = new User(randomUsername, randomEmail);
        User savedUser = userRepository.save(newUser);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo(randomUsername);
        assertThat(savedUser.getEmail()).isEqualTo(randomEmail);
        assertThat(savedUser.getCreatedAt()).isNotNull();

        var retrieved = userRepository.findByUsername(randomUsername);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void findByUsernameReturnsEmptyWhenNotFound() {
        var found = userRepository.findByUsername("nonexistent_user");

        assertThat(found).isEmpty();
    }

    @Test
    void userRepositoryIsAvailable() {
        assertThat(userRepository).isNotNull();
    }

    @Test
    void canInstantiateUserWithNoArgsConstructor() {
        User user = new User();
        assertThat(user).isNotNull();
    }

    @Test
    void userGettersAndSettersWork() {
        User user = new User();
        user.setId(1L);
        user.setUsername(generateRandomUsername());
        user.setEmail(generateRandomEmail());

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isNotNull();
        assertThat(user.getEmail()).isNotNull();
    }

    @Test
    void userToStringWorks() {
        User user = new User(generateRandomUsername(), generateRandomEmail());
        String str = user.toString();

        assertThat(str).contains("User{");
        assertThat(str).contains("username=");
        assertThat(str).contains("email=");
    }

    @Test
    void canUpdateExistingUser() {
        String username = generateRandomUsername();
        String email = generateRandomEmail();
        User user = new User(username, email);
        User saved = userRepository.save(user);

        String newEmail = generateRandomEmail();
        saved.setEmail(newEmail);
        User updated = userRepository.save(saved);

        assertThat(updated.getEmail()).isEqualTo(newEmail);
        assertThat(updated.getId()).isEqualTo(saved.getId());
    }

    @Test
    void canCountUsers() {
        long countBefore = userRepository.count();
        String username = generateRandomUsername();
        String email = generateRandomEmail();
        userRepository.save(new User(username, email));
        long countAfter = userRepository.count();

        assertThat(countAfter).isGreaterThan(countBefore);
    }

    @Test
    void canDeleteUserById() {
        String username = generateRandomUsername();
        String email = generateRandomEmail();
        User user = userRepository.save(new User(username, email));
        Long userId = user.getId();

        userRepository.deleteById(userId);

        var deleted = userRepository.findById(userId);
        assertThat(deleted).isEmpty();
    }

    @Test
    void canCheckIfUserExists() {
        String username = generateRandomUsername();
        String email = generateRandomEmail();
        userRepository.save(new User(username, email));

        var foundUser = userRepository.findByUsername(username);
        assertThat(foundUser).isPresent();
        assertThat(userRepository.existsById(foundUser.get().getId())).isTrue();
        assertThat(userRepository.existsById(99999L)).isFalse();
    }

    @Test
    void userCreatedAtFieldWorks() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);

        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    // Negative Tests
    @Test
    void findByUsernameReturnsEmptyForNonExistentUser() {
        var found = userRepository.findByUsername("this_user_definitely_does_not_exist_" + UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdReturnsEmptyForNonExistentId() {
        var found = userRepository.findById(99999L);

        assertThat(found).isEmpty();
    }

    @Test
    void cannotSaveDuplicateUsername() {
        String username = generateRandomUsername();
        String email1 = generateRandomEmail();
        String email2 = generateRandomEmail();

        userRepository.save(new User(username, email1));

        User duplicate = new User(username, email2);
        assertThat(
            org.assertj.core.api.Assertions.catchThrowable(() -> userRepository.save(duplicate))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cannotSaveDuplicateEmail() {
        String email = generateRandomEmail();
        String username1 = generateRandomUsername();
        String username2 = generateRandomUsername();

        userRepository.save(new User(username1, email));

        User duplicate = new User(username2, email);
        assertThat(
            org.assertj.core.api.Assertions.catchThrowable(() -> userRepository.save(duplicate))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingNonExistentUserDoesNotThrow() {
        assertThat(
            org.assertj.core.api.Assertions.catchThrowable(() -> userRepository.deleteById(99999L))
        ).doesNotThrowAnyException();
    }
}

