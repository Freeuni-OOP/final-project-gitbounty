package org.gitbounty.gitbountybackend.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private String randomUsername() {
        return "svc_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String randomEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@svc.test";
    }

    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private User user(Long id, String username, String email, String keycloakId) {
        User user = new User(username, email, keycloakId);
        user.setId(id);
        return user;
    }

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
        String keycloakId = randomKeycloakId();
        String username = randomUsername();
        String email = randomEmail();
        Jwt jwt = createMockJwt(keycloakId, username, email);

        when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.syncKeycloakUser(jwt);

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getUsername().equals(username)
                        && savedUser.getEmail().equals(email)
                        && savedUser.getKeycloakId().equals(keycloakId)
        ));
    }

    @Test
    void syncKeycloakUserDoesNotSaveWhenUserAlreadyExists() {
        String keycloakId = randomKeycloakId();
        Jwt jwt = createMockJwt(keycloakId, randomUsername(), randomEmail());

        when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(true);

        userService.syncKeycloakUser(jwt);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void syncKeycloakUserThrowsDataIntegrityViolationWhenSaveFails() {
        String keycloakId = randomKeycloakId();
        Jwt jwt = createMockJwt(keycloakId, randomUsername(), randomEmail());

        when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("duplicate"));

        assertThatThrownBy(() -> userService.syncKeycloakUser(jwt))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("couldn't save user");
    }

    @Test
    void createUserSucceedsWhenUsernameAndEmailAreUnique() throws Exception {
        String username = randomUsername();
        String email = randomEmail();
        String keycloakId = randomKeycloakId();

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        User created = userService.createUser(username, email, keycloakId);

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getUsername()).isEqualTo(username);
        assertThat(created.getEmail()).isEqualTo(email);
        assertThat(created.getKeycloakId()).isEqualTo(keycloakId);
    }

    @Test
    void createUserFailsWhenUsernameAlreadyExists() {
        String username = randomUsername();

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user(1L, username, randomEmail(), randomKeycloakId())));

        assertThatThrownBy(() -> userService.createUser(username, randomEmail(), randomKeycloakId()))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserFailsWhenEmailAlreadyExists() {
        String email = randomEmail();

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user(1L, randomUsername(), email, randomKeycloakId())));

        assertThatThrownBy(() -> userService.createUser(randomUsername(), email, randomKeycloakId()))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByIdReturnsUserWhenExists() {
        User existing = user(1L, randomUsername(), randomEmail(), randomKeycloakId());

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        Optional<User> found = userService.findById(1L);

        assertThat(found).contains(existing);
    }

    @Test
    void findByUsernameDelegatesToRepository() {
        User existing = user(1L, "demo", "demo@test.com", "kc-demo");

        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(existing));

        assertThat(userService.findByUsername("demo")).contains(existing);
    }

    @Test
    void findByKeycloakIdDelegatesToRepository() {
        User existing = user(1L, "demo", "demo@test.com", "kc-demo");

        when(userRepository.findByKeycloakId("kc-demo")).thenReturn(Optional.of(existing));

        assertThat(userService.findByKeycloakId("kc-demo")).contains(existing);
    }

    @Test
    void saveDelegatesToRepository() {
        User user = user(1L, randomUsername(), randomEmail(), randomKeycloakId());

        when(userRepository.save(user)).thenReturn(user);

        assertThat(userService.save(user)).isSameAs(user);
    }

    @Test
    void deleteDelegatesToRepository() {
        User user = user(1L, randomUsername(), randomEmail(), randomKeycloakId());

        userService.delete(user);

        verify(userRepository).delete(user);
    }

    @Test
    void updateUserProfileUpdatesUsernameAndEmailWhenUnique() throws Exception {
        User existing = user(1L, "old-user", "old@test.com", "kc-old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateUserProfile(1L, "new-user", "new@test.com");

        assertThat(updated.getUsername()).isEqualTo("new-user");
        assertThat(updated.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void updateUserProfileKeepsExistingValuesWhenNewValuesAreNullOrBlank() throws Exception {
        User existing = user(1L, "old-user", "old@test.com", "kc-old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateUserProfile(1L, "   ", null);

        assertThat(updated.getUsername()).isEqualTo("old-user");
        assertThat(updated.getEmail()).isEqualTo("old@test.com");

        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void updateUserProfileThrowsDuplicateUserWhenUsernameAlreadyExists() {
        User target = user(1L, "target", "target@test.com", "kc-target");
        User existing = user(2L, "existing", "existing@test.com", "kc-existing");

        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.updateUserProfile(1L, "existing", null))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfileThrowsDuplicateUserWhenEmailAlreadyExists() {
        User target = user(1L, "target", "target@test.com", "kc-target");
        User existing = user(2L, "existing", "existing@test.com", "kc-existing");

        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.updateUserProfile(1L, null, "existing@test.com"))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfileThrowsUserNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserProfile(Long.MAX_VALUE, randomUsername(), randomEmail()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id");
    }
}
