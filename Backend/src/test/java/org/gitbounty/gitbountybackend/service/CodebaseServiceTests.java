package org.gitbounty.gitbountybackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Optional;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CodebaseRepository;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CodebaseServiceTests {

    @TempDir
    Path repositoriesRoot;

    @Test
    void createCodebaseCreatesBareRepositoryAndPersistsCodebase() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CodebaseService codebaseService = new CodebaseService(codebaseRepository, userRepository, repositoriesRoot);
        User owner = new User("git-owner", "git-owner@test.local", "encoded-password");
        Principal principal = () -> "git-owner";

        when(userRepository.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Codebase created = codebaseService.createCodebase(
            "demo.git",
            "Demo repository",
            "http://localhost/git/demo.git",
            principal
        );

        assertThat(created.getName()).isEqualTo("demo.git");
        assertThat(created.getDescription()).isEqualTo("Demo repository");
        assertThat(created.getGitUrl()).isEqualTo("http://localhost/git/demo.git");
        assertThat(created.getOwner().getUsername()).isEqualTo("git-owner");
        assertThat(Files.exists(repositoriesRoot.resolve("demo.git"))).isTrue();
    }

    @Test
    void createCodebaseCleansUpRepositoryDirectoryWhenPersistenceFails() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CodebaseService codebaseService = new CodebaseService(codebaseRepository, userRepository, repositoriesRoot);
        User owner = new User("git-owner", "git-owner@test.local", "encoded-password");
        Principal principal = () -> "git-owner";

        when(userRepository.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> codebaseService.createCodebase(
            "demo.git",
            "Demo repository",
            "http://localhost/git/demo.git",
            principal
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("boom");

        assertThat(Files.exists(repositoriesRoot.resolve("demo.git"))).isFalse();
    }

    @Test
    void createCodebaseRejectsDuplicateRepositoryNames() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CodebaseService codebaseService = new CodebaseService(codebaseRepository, userRepository, repositoriesRoot);
        User owner = new User("git-owner", "git-owner@test.local", "encoded-password");
        Principal principal = () -> "git-owner";

        when(userRepository.findByUsername("git-owner")).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo.git")).thenReturn(
            Optional.of(new Codebase("demo.git", "Existing", "http://localhost/git/demo.git", owner))
        );

        assertThatThrownBy(() -> codebaseService.createCodebase(
            "demo.git",
            "Demo repository",
            "http://localhost/git/demo.git",
            principal
        ))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> {
                ResponseStatusException statusException = (ResponseStatusException) error;
                assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            });
    }

    @Test
    void createCodebaseRejectsNullPrincipal() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CodebaseService codebaseService = new CodebaseService(codebaseRepository, userRepository, repositoriesRoot);

        assertThatThrownBy(() -> codebaseService.createCodebase(
            "demo.git",
            "Demo repository",
            "http://localhost/git/demo.git",
            null
        ))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> {
                ResponseStatusException statusException = (ResponseStatusException) error;
                assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
    }
}

