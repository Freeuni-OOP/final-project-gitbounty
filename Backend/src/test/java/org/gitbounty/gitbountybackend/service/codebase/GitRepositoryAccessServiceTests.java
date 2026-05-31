package org.gitbounty.gitbountybackend.service.codebase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.UUID;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GitRepositoryAccessServiceTests {

    @Test
    void ownerCanWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository);
        Principal principal = () -> "git-owner";
        User owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
        Codebase codebase = new Codebase("demo", "Demo repository", "http://localhost/git/demo.git", owner);

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));
            when(codebaseRepository.findByName("demo")).thenReturn(java.util.Optional.of(codebase));

            assertThatCode(() -> accessService.assertOwnerCanWrite(repository, principal))
                .doesNotThrowAnyException();
        }
    }
    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
    }
    @Test
    void nonOwnerCannotWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository);
        Principal principal = () -> "git-intruder";
        User owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
        Codebase codebase = new Codebase("demo", "Demo repository", "http://localhost/git/demo.git", owner);

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));
            when(codebaseRepository.findByName("demo")).thenReturn(java.util.Optional.of(codebase));

            assertThatThrownBy(() -> accessService.assertOwnerCanWrite(repository, principal))
                .isInstanceOf(ServiceNotAuthorizedException.class)
                .hasMessageContaining("Only the repository owner may push");
        }
    }

    @Test
    void nullPrincipalCannotWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository);

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));

            assertThatThrownBy(() -> accessService.assertOwnerCanWrite(repository, null))
                .isInstanceOf(ServiceNotAuthorizedException.class)
                .hasMessageContaining("Authentication is required to push");
        }
    }
}



