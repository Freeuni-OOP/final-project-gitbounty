package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodebasePermissionsTest {

    @Mock
    private CodebaseService codebaseService;

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
}