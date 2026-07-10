package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodebasePermissionsTest {

    @Mock
    private CodebaseService codebaseService;

    @Mock
    private CodebaseMemberRepository codebaseMemberRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CodebasePermissions codebasePermissions;

    private static Codebase codebaseOwnedBy(String username, String keycloakId) {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername(username);
        owner.setEmail(username + "@test.com");
        owner.setKeycloakId(keycloakId);

        Codebase codebase = new Codebase();
        codebase.setId(10L);
        codebase.setName("my-repo");
        codebase.setOwner(owner);

        return codebase;
    }

    @Test
    void isOwner_ShouldReturnTrue_WhenAuthenticatedUsernameMatchesOwner() {
        when(codebaseService.getCodebase("my-repo"))
                .thenReturn(codebaseOwnedBy("owner", "kc-owner"));

        assertThat(codebasePermissions.isOwner("my-repo", "owner")).isTrue();

        verify(codebaseService).getCodebase("my-repo");
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenAuthenticatedUsernameDoesNotMatchOwner() {
        when(codebaseService.getCodebase("my-repo"))
                .thenReturn(codebaseOwnedBy("owner", "kc-owner"));

        assertThat(codebasePermissions.isOwner("my-repo", "other")).isFalse();
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenInputInvalid() {
        assertThat(codebasePermissions.isOwner(null, "owner")).isFalse();
        assertThat(codebasePermissions.isOwner("   ", "owner")).isFalse();
        assertThat(codebasePermissions.isOwner("my-repo", null)).isFalse();
        assertThat(codebasePermissions.isOwner("my-repo", "   ")).isFalse();

        verifyNoInteractions(codebaseService);
    }

    @Test
    void isOwnerBySubject_ShouldReturnTrue_WhenSubjectMatchesOwnerKeycloakId() {
        when(codebaseService.getCodebase("my-repo"))
                .thenReturn(codebaseOwnedBy("owner", "kc-owner"));

        assertThat(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).isTrue();

        verify(codebaseService).getCodebase("my-repo");
    }

    @Test
    void isOwnerBySubject_ShouldReturnFalse_WhenSubjectDoesNotMatchOwnerKeycloakId() {
        when(codebaseService.getCodebase("my-repo"))
                .thenReturn(codebaseOwnedBy("owner", "kc-owner"));

        assertThat(codebasePermissions.isOwnerBySubject("my-repo", "kc-other")).isFalse();
    }

    @Test
    void canDeleteRepository_ShouldReturnTrue_WhenSubjectIsOwner() {
        when(codebaseService.findById(10L)).thenReturn(codebaseOwnedBy("owner", "kc-owner"));

        assertThat(codebasePermissions.canDeleteRepository(10L, "kc-owner")).isTrue();

        verifyNoInteractions(userService, codebaseMemberRepository);
    }

    @ParameterizedTest
    @EnumSource(value = CodebaseRole.class, names = {"OWNER", "MAINTAINER", "DEVELOPER"})
    void canDeleteRepository_ShouldReturnTrue_WhenMemberHasWriteRole(CodebaseRole role) {
        Codebase codebase = codebaseOwnedBy("owner", "kc-owner");
        User member = new User();
        member.setId(2L);
        member.setKeycloakId("kc-member");

        when(codebaseService.findById(10L)).thenReturn(codebase);
        when(userService.findByKeycloakId("kc-member")).thenReturn(Optional.of(member));
        when(codebaseMemberRepository.findByCodebaseIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(CodebaseMember.builder().codebase(codebase).user(member).role(role).build()));

        assertThat(codebasePermissions.canDeleteRepository(10L, "kc-member")).isTrue();
    }

    @Test
    void canDeleteRepository_ShouldReturnFalse_WhenMemberIsReadOnlyReporter() {
        Codebase codebase = codebaseOwnedBy("owner", "kc-owner");
        User member = new User();
        member.setId(2L);
        member.setKeycloakId("kc-reporter");

        when(codebaseService.findById(10L)).thenReturn(codebase);
        when(userService.findByKeycloakId("kc-reporter")).thenReturn(Optional.of(member));
        when(codebaseMemberRepository.findByCodebaseIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(CodebaseMember.builder().codebase(codebase).user(member).role(CodebaseRole.REPORTER).build()));

        assertThat(codebasePermissions.canDeleteRepository(10L, "kc-reporter")).isFalse();
    }

    @Test
    void canDeleteRepository_ShouldReturnFalse_WhenSubjectIsNotAMember() {
        when(codebaseService.findById(10L)).thenReturn(codebaseOwnedBy("owner", "kc-owner"));
        when(userService.findByKeycloakId("kc-stranger")).thenReturn(Optional.empty());

        assertThat(codebasePermissions.canDeleteRepository(10L, "kc-stranger")).isFalse();
    }

    @Test
    void canDeleteRepository_ShouldReturnFalse_WhenInputInvalid() {
        assertThat(codebasePermissions.canDeleteRepository(null, "kc-owner")).isFalse();
        assertThat(codebasePermissions.canDeleteRepository(10L, null)).isFalse();
        assertThat(codebasePermissions.canDeleteRepository(10L, "  ")).isFalse();

        verifyNoInteractions(codebaseService, userService, codebaseMemberRepository);
    }
}