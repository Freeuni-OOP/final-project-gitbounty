package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.gitbounty.gitbountybackend.exception.ResourceNotFoundException;
import org.gitbounty.gitbountybackend.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PullRequestPersistenceServiceTests {

    @Mock
    private PullRequestRepository repository;

    @InjectMocks
    private PullRequestPersistenceService persistenceService;

    @Test
    void create_ShouldSaveAndFlush() {
        // Setup
        User author = new User();
        Codebase codebase = new Codebase();
        Branch source = new Branch();
        Branch target = new Branch();
        CreatePullRequestCommand command = new CreatePullRequestCommand(
            "repo", "user", "source", "target", "Title", "Desc"
        );

        when(repository.saveAndFlush(any(PullRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Execute
        PullRequest result = persistenceService.create(command, author, codebase, source, target, 1);

        // Verify
        assertThat(result.getTitle()).isEqualTo("Title");
        verify(repository).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void finalizeMerge_ShouldUpdateStatusAndSave() {
        // Setup
        PullRequest pr = new PullRequest();
        pr.setId(99L);
        when(repository.findById(99L)).thenReturn(Optional.of(pr));
        when(repository.save(any(PullRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Execute
        PullRequest result = persistenceService.finalizeMerge(99L);

        // Verify
        assertThat(result.getStatus()).isEqualTo(IssueStatus.CLOSED);
        assertThat(result.getMergedAt()).isNotNull();
        verify(repository).save(pr);
    }

    @Test
    void finalizeMerge_ThrowsException_WhenNotFound() {
        // Setup
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // Execute & Verify
        assertThatThrownBy(() -> persistenceService.finalizeMerge(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}