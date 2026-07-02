package org.gitbounty.gitbountybackend.controller.bounty;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.repository.BountyRepository;

@ExtendWith(MockitoExtension.class)
class BountyPermissionsTest {

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private BountyPermissions bountyPermissions;

    @Mock
    private BountyRepository bountyRepository;

    private static Bounty bountyOwnedBy(String keycloakId) {
        Bounty bounty = new Bounty();
        bounty.setId(20L);
        bounty.setIssue(issueOwnedBy(keycloakId));

        return bounty;
    }

    private static Issue issueOwnedBy(String keycloakId) {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setEmail("owner@test.com");
        owner.setKeycloakId(keycloakId);

        Codebase repository = new Codebase();
        repository.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(10L);
        issue.setRepository(repository);

        return issue;
    }

    @Test
    void isIssueRepositoryOwner_ShouldReturnTrue_WhenAuthenticatedUserOwnsRepository() {
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issueOwnedBy("kc-owner")));

        assertThat(bountyPermissions.isIssueRepositoryOwner(10L, "kc-owner")).isTrue();

        verify(issueRepository).findById(10L);
    }

    @Test
    void isIssueRepositoryOwner_ShouldReturnFalse_WhenAuthenticatedUserDoesNotOwnRepository() {
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issueOwnedBy("kc-owner")));

        assertThat(bountyPermissions.isIssueRepositoryOwner(10L, "kc-other")).isFalse();
    }

    @Test
    void isIssueRepositoryOwner_ShouldReturnFalse_WhenIssueMissing() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(bountyPermissions.isIssueRepositoryOwner(99L, "kc-owner")).isFalse();
    }

    @Test
    void isIssueRepositoryOwner_ShouldReturnFalse_WhenOwnerKeycloakIdIsNull() {
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issueOwnedBy(null)));

        assertThat(bountyPermissions.isIssueRepositoryOwner(10L, "kc-owner")).isFalse();
    }

    @Test
    void isIssueRepositoryOwner_ShouldReturnFalse_WhenInputInvalid() {
        assertThat(bountyPermissions.isIssueRepositoryOwner(null, "kc-owner")).isFalse();
        assertThat(bountyPermissions.isIssueRepositoryOwner(10L, null)).isFalse();
        assertThat(bountyPermissions.isIssueRepositoryOwner(10L, "   ")).isFalse();

        verifyNoInteractions(issueRepository);
    }

    @Test
    void isBountyRepositoryOwner_ShouldReturnTrue_WhenAuthenticatedUserOwnsRepository() {
        when(bountyRepository.findById(20L)).thenReturn(Optional.of(bountyOwnedBy("kc-owner")));
        assertThat(bountyPermissions.isBountyRepositoryOwner(20L, "kc-owner")).isTrue();

        verify(bountyRepository).findById(20L);
    }

    @Test
    void isBountyRepositoryOwner_ShouldReturnFalse_WhenAuthenticatedUserDoesNotOwnRepository() {
        when(bountyRepository.findById(20L)).thenReturn(Optional.of(bountyOwnedBy("kc-owner")));

        assertThat(bountyPermissions.isBountyRepositoryOwner(20L, "kc-other")).isFalse();
    }

    @Test
    void isBountyRepositoryOwner_ShouldReturnFalse_WhenBountyMissing() {
        when(bountyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(bountyPermissions.isBountyRepositoryOwner(99L, "kc-owner")).isFalse();
    }

    @Test
    void isBountyRepositoryOwner_ShouldReturnFalse_WhenInputInvalid() {
        assertThat(bountyPermissions.isBountyRepositoryOwner(null, "kc-owner")).isFalse();
        assertThat(bountyPermissions.isBountyRepositoryOwner(20L, null)).isFalse();
        assertThat(bountyPermissions.isBountyRepositoryOwner(20L, "   ")).isFalse();

        verifyNoInteractions(bountyRepository);
    }
}