package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.gitbounty.gitbountybackend.exception.ResourceNotFoundException;
import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PullRequestPersistenceServiceTests {

    @Mock
    private PullRequestRepository repository;

    @Mock
    private BountyService bountyService;

    @InjectMocks
    private PullRequestPersistenceService persistenceService;

    @Test
    void create_ShouldSaveAndFlush() {
        User author = new User();
        Codebase codebase = new Codebase();
        Branch source = new Branch();
        Branch target = new Branch();
        CreatePullRequestCommand command = new CreatePullRequestCommand(
                "repo", "user", "source", "target", "Title", "Desc"
        );

        when(repository.saveAndFlush(any(PullRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PullRequest result = persistenceService.create(command, author, codebase, source, target, 1);

        assertThat(result.getTitle()).isEqualTo("Title");

        verify(repository).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void finalizeMerge_ShouldUpdateStatusAndSave_WhenNoBounty() {
        // PR without a bounty should still close normally
        PullRequest pr = new PullRequest();
        pr.setId(99L);
        pr.setStatus(IssueStatus.OPEN);

        when(repository.findById(99L)).thenReturn(Optional.of(pr));
        when(repository.saveAndFlush(pr)).thenReturn(pr);

        PullRequest result = persistenceService.finalizeMerge(99L);

        assertThat(result.getStatus()).isEqualTo(IssueStatus.CLOSED);
        assertThat(result.getMergedAt()).isNotNull();

        verify(repository).saveAndFlush(pr);
        verifyNoInteractions(bountyService);
    }

    @Test
    void finalizeMerge_ShouldPayAuthorAndClose_WhenBountyActive() {
        // Merging a funded PR should pay its author and close it.
        User author = new User();
        author.setId(2L);

        Bounty bounty = new Bounty();
        bounty.setId(20L);
        bounty.setStatus(BountyStatus.OPEN);

        PullRequest pr = new PullRequest();
        pr.setId(99L);
        pr.setStatus(IssueStatus.OPEN);
        pr.setAuthor(author);
        pr.setBounty(bounty);

        when(repository.findById(99L)).thenReturn(Optional.of(pr));
        when(repository.saveAndFlush(pr)).thenReturn(pr);

        PullRequest result = persistenceService.finalizeMerge(99L);

        verify(bountyService).completeBountyAndPayRecipient(20L, author);

        assertThat(result.getStatus()).isEqualTo(IssueStatus.CLOSED);
        assertThat(result.getMergedAt()).isNotNull();

        verify(repository).saveAndFlush(pr);
    }

    @Test
    void updatePRStatus_ShouldCancelBounty_WhenClosingWithoutMerge() {
        // Closing without merging should refund the active bounty.
        Bounty bounty = new Bounty();
        bounty.setId(20L);
        bounty.setStatus(BountyStatus.OPEN);

        PullRequest pr = new PullRequest();
        pr.setId(99L);
        pr.setStatus(IssueStatus.OPEN);
        pr.setBounty(bounty);
        pr.setMergedAt(null);

        when(repository.findById(99L)).thenReturn(Optional.of(pr));
        when(repository.saveAndFlush(pr)).thenReturn(pr);

        persistenceService.updatePRStatus(99L, IssueStatus.CLOSED);

        verify(bountyService).cancelBounty(20L);

        assertThat(pr.getStatus()).isEqualTo(IssueStatus.CLOSED);
        assertThat(pr.getMergedAt()).isNull();

        verify(repository).saveAndFlush(pr);
    }

    @Test
    void updatePRStatus_ShouldNotCancelCompletedBounty() {
        // A completed bounty must never be refunded again.
        Bounty bounty = new Bounty();
        bounty.setId(20L);
        bounty.setStatus(BountyStatus.COMPLETED);

        PullRequest pr = new PullRequest();
        pr.setId(99L);
        pr.setStatus(IssueStatus.OPEN);
        pr.setBounty(bounty);

        when(repository.findById(99L)).thenReturn(Optional.of(pr));
        when(repository.saveAndFlush(pr)).thenReturn(pr);

        persistenceService.updatePRStatus(99L, IssueStatus.CLOSED);

        verify(bountyService, never()).cancelBounty(anyLong());

        assertThat(pr.getStatus()).isEqualTo(IssueStatus.CLOSED);

        verify(repository).saveAndFlush(pr);
    }

    @Test
    void finalizeMerge_ShouldNotPayTwice_WhenAlreadyMerged() {
        // Repeating a merge must not trigger a second payment.
        PullRequest pr = new PullRequest();
        pr.setId(99L);
        pr.setStatus(IssueStatus.CLOSED);
        pr.setMergedAt(Instant.now());

        when(repository.findById(99L)).thenReturn(Optional.of(pr));

        assertThatThrownBy(() ->
                persistenceService.finalizeMerge(99L)
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(bountyService);

        verify(repository, never()).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void finalizeMerge_ThrowsException_WhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                persistenceService.finalizeMerge(99L)
        ).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(bountyService);

        verify(repository, never()).saveAndFlush(any(PullRequest.class));
    }
}