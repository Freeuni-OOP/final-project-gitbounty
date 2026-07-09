package org.gitbounty.gitbountybackend.service.codebase;

import jakarta.persistence.EntityManager;
import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mockito-based unit tests for CodebaseDeletionCascadeService.deleteRepositoryRecords. These
 * exercise call order and per-entity delegation only - the actual Hibernate flush behavior
 * can't be verified with mocks and is covered separately by
 * CodebaseDeletionCascadeServicePersistenceTests (real H2 persistence context).
 */
@ExtendWith(MockitoExtension.class)
class CodebaseDeletionCascadeServiceTests {

    @Mock
    private CodebaseRepository codebaseRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private BountyService bountyService;

    private CodebaseDeletionCascadeService cascadeService;

    private Codebase codebase;

    @BeforeEach
    void setUp() {
        cascadeService = new CodebaseDeletionCascadeService(codebaseRepository, branchService, transactionService,
                issueRepository, pullRequestRepository, entityManager, bountyService);

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setKeycloakId("kc-owner");

        codebase = new Codebase("demo", "desc", "url", owner);
        codebase.setId(10L);

        lenient().when(codebaseRepository.findById(10L)).thenReturn(Optional.of(codebase));
        lenient().when(pullRequestRepository.findByRepository(codebase)).thenReturn(List.of());
        lenient().when(issueRepository.findByRepositoryId(10L)).thenReturn(List.of());
    }

    @Test
    void deleteRepositoryRecords_ShouldDeleteCleanly_WhenRepoHasNoIssuesOrBounties() {
        Codebase result = cascadeService.deleteRepositoryRecords(10L);

        assertThat(result).isEqualTo(codebase);
        verify(transactionService).detachBountyReferencesForRepository(10L);
        verify(branchService).deleteAllBranchesForCodebase(codebase);
        verify(codebaseRepository).deleteById(10L);
    }

    @Test
    void deleteRepositoryRecords_ShouldDetachTransactionsBeforeDeletingIssuesAndBranches() {
        cascadeService.deleteRepositoryRecords(10L);

        InOrder order = inOrder(transactionService, pullRequestRepository, issueRepository, branchService,
                entityManager, codebaseRepository);
        order.verify(transactionService).detachBountyReferencesForRepository(10L);
        // Flushed+cleared here on purpose: the detach above must reach the database before
        // the entity-level deletes below run, or deleting a bounty while a transaction still
        // references it (in the DB) would violate that foreign key. The clear() afterward
        // drops now-stale entities from the persistence context so the next phase's flush
        // never has to reason about two connected entities disappearing at once (see
        // CodebaseDeletionCascadeService.deleteRepositoryRecords for the full explanation).
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(pullRequestRepository).findByRepository(codebase);
        order.verify(issueRepository).findByRepositoryId(10L);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(branchService).deleteAllBranchesForCodebase(codebase);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(codebaseRepository).deleteById(10L);
    }

    @Test
    void deleteRepositoryRecords_ShouldDeleteEachPullRequestAndIssueEntityIndividually() {
        PullRequest pr = mock(PullRequest.class);
        Issue issueWithBounty = new Issue();
        issueWithBounty.setId(100L);
        Issue issueWithoutBounty = new Issue();
        issueWithoutBounty.setId(101L);

        when(pullRequestRepository.findByRepository(codebase)).thenReturn(List.of(pr));
        when(issueRepository.findByRepositoryId(10L)).thenReturn(List.of(issueWithBounty, issueWithoutBounty));

        cascadeService.deleteRepositoryRecords(10L);

        // Entity-level removal (never a bulk query), so a JOINED-inheritance bulk delete
        // never has to touch Hibernate's temp-table strategy.
        verify(pullRequestRepository).delete(pr);
        verify(issueRepository).delete(issueWithBounty);
        verify(issueRepository).delete(issueWithoutBounty);
        verify(pullRequestRepository, never()).deleteAll(any());
        verify(issueRepository, never()).deleteAll(any());
    }

    @Test
    void deleteRepositoryRecords_ShouldCancelEachIssuesBountyImmediatelyBeforeDeletingThatIssue() {
        Issue issue1 = new Issue();
        issue1.setId(100L);
        Bounty bounty1 = mock(Bounty.class);
        issue1.setBounty(bounty1);

        Issue issue2 = new Issue();
        issue2.setId(101L);
        // No bounty on issue2 - cancelIfActive must still be called (with null) rather than
        // skipped, so BountyService alone decides what "active" means.

        when(issueRepository.findByRepositoryId(10L)).thenReturn(List.of(issue1, issue2));

        cascadeService.deleteRepositoryRecords(10L);

        InOrder order = inOrder(bountyService, issueRepository);
        order.verify(bountyService).cancelIfActive(bounty1);
        order.verify(issueRepository).delete(issue1);
        order.verify(bountyService).cancelIfActive(null);
        order.verify(issueRepository).delete(issue2);

        // Never an upfront "cancel everything" pass before the loop - each cancellation is
        // scoped to, and ordered immediately before, its own issue's deletion.
        verify(bountyService, times(2)).cancelIfActive(any());
    }

    @Test
    void deleteRepositoryRecords_ShouldThrowNotFound_WhenRepositoryMissing() {
        when(codebaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cascadeService.deleteRepositoryRecords(999L))
                .isInstanceOf(CodebaseNotFoundException.class);

        verifyNoInteractions(transactionService, issueRepository, pullRequestRepository, branchService, entityManager);
        verify(codebaseRepository, never()).deleteById(any());
    }
}
