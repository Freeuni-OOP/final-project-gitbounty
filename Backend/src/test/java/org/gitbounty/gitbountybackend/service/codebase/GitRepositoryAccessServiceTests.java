package org.gitbounty.gitbountybackend.service.codebase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GitRepositoryAccessServiceTests {

    @Test
    void ownerCanWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        CodebaseMemberRepository memberRepository = Mockito.mock(CodebaseMemberRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository, memberRepository);
        Principal principal = () -> "git-owner";
        User owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
        Codebase codebase = new Codebase("demo", "Demo repository", "http://localhost/git/demo.git", owner);

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));
            when(codebaseRepository.findByName("demo")).thenReturn(java.util.Optional.of(codebase));
            when(memberRepository.findByCodebaseId(codebase.getId())).thenReturn(List.of());

            assertThatCode(() -> accessService.assertUserCanWrite(repository, principal))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void memberWithDeveloperRoleCanWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        CodebaseMemberRepository memberRepository = Mockito.mock(CodebaseMemberRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository, memberRepository);
        Principal principal = () -> "git-developer";
        User owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
        User developer = new User("git-developer", "git-developer@test.local", randomKeycloakId());
        Codebase codebase = new Codebase("demo", "Demo repository", "http://localhost/git/demo.git", owner);
        CodebaseMember member = CodebaseMember.builder()
                .codebase(codebase)
                .user(developer)
                .role(CodebaseRole.DEVELOPER)
                .build();

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));
            when(codebaseRepository.findByName("demo")).thenReturn(java.util.Optional.of(codebase));
            when(memberRepository.findByCodebaseId(codebase.getId())).thenReturn(List.of(member));

            assertThatCode(() -> accessService.assertUserCanWrite(repository, principal))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void memberWithMaintainerRoleCanWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        CodebaseMemberRepository memberRepository = Mockito.mock(CodebaseMemberRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository, memberRepository);
        Principal principal = () -> "git-maintainer";
        User owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
        User maintainer = new User("git-maintainer", "git-maintainer@test.local", randomKeycloakId());
        Codebase codebase = new Codebase("demo", "Demo repository", "http://localhost/git/demo.git", owner);
        CodebaseMember member = CodebaseMember.builder()
                .codebase(codebase)
                .user(maintainer)
                .role(CodebaseRole.MAINTAINER)
                .build();

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));
            when(codebaseRepository.findByName("demo")).thenReturn(java.util.Optional.of(codebase));
            when(memberRepository.findByCodebaseId(codebase.getId())).thenReturn(List.of(member));

            assertThatCode(() -> accessService.assertUserCanWrite(repository, principal))
                .doesNotThrowAnyException();
        }
    }

    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void nonOwnerCannotWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        CodebaseMemberRepository memberRepository = Mockito.mock(CodebaseMemberRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository, memberRepository);
        Principal principal = () -> "git-intruder";
        User owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
        Codebase codebase = new Codebase("demo", "Demo repository", "http://localhost/git/demo.git", owner);

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));
            when(codebaseRepository.findByName("demo")).thenReturn(java.util.Optional.of(codebase));
            when(memberRepository.findByCodebaseId(codebase.getId())).thenReturn(List.of());

            assertThatThrownBy(() -> accessService.assertUserCanWrite(repository, principal))
                .isInstanceOf(ServiceNotAuthorizedException.class)
                .hasMessageContaining("Only repository owners and members may push");
        }
    }

    @Test
    void nullPrincipalCannotWriteToRepository() {
        CodebaseRepository codebaseRepository = Mockito.mock(CodebaseRepository.class);
        CodebaseMemberRepository memberRepository = Mockito.mock(CodebaseMemberRepository.class);
        GitRepositoryAccessService accessService = new GitRepositoryAccessService(codebaseRepository, memberRepository);

        try (Repository repository = Mockito.mock(Repository.class)) {
            when(repository.getDirectory()).thenReturn(new java.io.File("/tmp/demo.git"));

            assertThatThrownBy(() -> accessService.assertUserCanWrite(repository, null))
                .isInstanceOf(ServiceNotAuthorizedException.class)
                .hasMessageContaining("Authentication is required to push");
        }
    }
}



