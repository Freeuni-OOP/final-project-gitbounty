package org.gitbounty.gitbountybackend.service.codebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodebaseServiceTests {

    private CodebaseRepository codebaseRepository;
    private UserService userService;
    private CodebaseStorageService storageService;
    private CodebaseService codebaseService;
    private User owner;
    private Principal principal;

    @BeforeAll
    void initSuite() {
        codebaseRepository = Mockito.mock(CodebaseRepository.class);
        userService = Mockito.mock(UserService.class);
        storageService = Mockito.mock(CodebaseStorageService.class);
        codebaseService = new CodebaseService(codebaseRepository, storageService, userService);
        owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
        principal = () -> "git-owner";
    }

    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void resetMocks() {
        reset(codebaseRepository, userService, storageService);
    }

    @Test
    void createCodebaseCreatesStorageAndPersistsCodebase() {
        when(userService.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Codebase created = codebaseService.createCodebase("demo.git", "Demo repository", "http://localhost/git/demo.git", principal);

        assertThat(created.getName()).isEqualTo("demo.git");
        assertThat(created.getDescription()).isEqualTo("Demo repository");
        assertThat(created.getGitUrl()).isEqualTo("http://localhost/git/demo.git");
        assertThat(created.getOwner().getUsername()).isEqualTo("git-owner");
        verify(storageService).createRepository("demo.git");
    }

    @Test
    void createCodebaseDeletesStorageWhenPersistenceFails() {
        when(userService.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> codebaseService.createCodebase("demo.git", "Demo repository", "http://localhost/git/demo.git", principal))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("boom");

        verify(storageService).createRepository("demo.git");
        verify(storageService).deleteRepository("demo.git");
    }

    @Test
    void createCodebaseKeepsOriginalExceptionWhenCleanupFails() {
        when(userService.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenThrow(new IllegalStateException("boom"));
        Mockito.doThrow(new RuntimeException("cleanup-failed")).when(storageService).deleteRepository("demo.git");

        Throwable thrown = catchThrowable(() ->
            codebaseService.createCodebase("demo.git", "Demo repository", "http://localhost/git/demo.git", principal)
        );

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getSuppressed()).hasSize(1);
        assertThat(thrown.getSuppressed()[0].getMessage()).isEqualTo("cleanup-failed");
    }

    @Test
    void createCodebaseRejectsDuplicateRepositoryNames() {
        when(userService.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(
            Optional.of(new Codebase("demo.git", "Existing", "http://localhost/git/demo.git", owner))
        );

        assertThatThrownBy(() -> codebaseService.createCodebase("demo.git", "Demo repository", "http://localhost/git/demo.git", principal))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(storageService, never()).createRepository(any());
    }

    @Test
    void createCodebaseRejectsNullPrincipal() {
        assertThatThrownBy(() -> codebaseService.createCodebase("demo.git", "Demo repository", "http://localhost/git/demo.git", null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(storageService, never()).createRepository(any());
    }

    @Test
    void createCodebaseRejectsPathSeparatorsInName() {
        assertThatThrownBy(() -> codebaseService.createCodebase("demo/evil.git", "Demo repository", "http://localhost/git/demo.git", () -> "git-owner"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(storageService, never()).createRepository(any());
    }

    @Test
    void createCodebaseRejectsUnknownAuthenticatedUser() {
        Principal principal = () -> "missing-user";
        when(userService.findByUsername("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> codebaseService.createCodebase("demo.git", "Demo repository", "http://localhost/git/demo.git", principal))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(storageService, never()).createRepository(any());
    }

    @Test
    void createCodebasePropagatesStorageConflict() {
        when(userService.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.empty());
        Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Repository directory already exists: demo.git"))
            .when(storageService)
            .createRepository("demo.git");

        assertThatThrownBy(() -> codebaseService.createCodebase("demo.git", "Demo repository", "http://localhost/git/demo.git", principal))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deleteCodebaseDeletesRecordAndStorage() {
        Codebase existing = new Codebase("demo.git", "desc", "url", owner);
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.of(existing));

        codebaseService.deleteCodebase("demo.git");

        verify(codebaseRepository).delete(existing);
        verify(storageService).deleteRepository("demo.git");
    }

    @Test
    void deleteCodebaseStillDeletesStorageWhenRecordMissing() {
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.empty());

        codebaseService.deleteCodebase("demo.git");

        verify(codebaseRepository, never()).delete(any());
        verify(storageService).deleteRepository("demo.git");
    }

    @Test
    void deleteCodebaseRejectsInvalidName() {
        assertThatThrownBy(() -> codebaseService.deleteCodebase("../evil"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(storageService, never()).deleteRepository(any());
    }
}
