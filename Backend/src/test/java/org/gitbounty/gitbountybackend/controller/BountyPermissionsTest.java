package org.gitbounty.gitbountybackend.controller;

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

@ExtendWith(MockitoExtension.class)
class BountyPermissionsTest {

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private BountyPermissions bountyPermissions;

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
}