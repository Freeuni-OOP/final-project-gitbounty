package org.gitbounty.gitbountybackend.service.codebase;

import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodebaseDeletionServiceTests {

    @Mock
    private CodebaseService codebaseService;

    @Mock
    private CodebaseRepository codebaseRepository;

    @Mock
    private BountyService bountyService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private EntityManager entityManager;

    private CodebaseDeletionService deletionService;

    private Codebase codebase;

    @BeforeEach
    void setUp() {
        deletionService = new CodebaseDeletionService(
                codebaseService, codebaseRepository, bountyService, transactionService, issueRepository, branchService,
                entityManager);

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setKeycloakId("kc-owner");

        codebase = new Codebase("demo", "desc", "url", owner);
        codebase.setId(10L);
    }

    @Test
    void deleteRepositoryRecords_ShouldDeleteCleanly_WhenRepoHasNoIssuesOrBounties() {
        when(codebaseService.findById(10L)).thenReturn(codebase);

        Codebase result = deletionService.deleteRepositoryRecords(10L);

        assertThat(result).isEqualTo(codebase);
        verify(bountyService).cancelActiveBountiesForRepository(10L);
        verify(transactionService).detachBountyReferencesForRepository(10L);
        verify(issueRepository).deletePullRequestsByRepositoryId(10L);
        verify(issueRepository).deleteIssuesByRepositoryId(10L);
        verify(branchService).deleteAllBranchesForCodebase(codebase);
        verify(codebaseRepository).deleteById(10L);
    }

    @Test
    void deleteRepositoryRecords_ShouldRefundActiveBountiesBeforeDeletingIssuesAndBranches() {
        when(codebaseService.findById(10L)).thenReturn(codebase);

        deletionService.deleteRepositoryRecords(10L);

        InOrder order = inOrder(bountyService, transactionService, issueRepository, branchService, entityManager, codebaseRepository);
        order.verify(bountyService).cancelActiveBountiesForRepository(10L);
        order.verify(transactionService).detachBountyReferencesForRepository(10L);
        // Flushed here on purpose: the refund/detach changes above must reach the DB before
        // the bulk deletes run their own SQL, or deleting a bounty while a transaction still
        // references it (in the DB) would violate that foreign key.
        order.verify(entityManager).flush();
        order.verify(issueRepository).deletePullRequestsByRepositoryId(10L);
        order.verify(issueRepository).deleteIssuesByRepositoryId(10L);
        order.verify(branchService).deleteAllBranchesForCodebase(codebase);
        // Cleared here on purpose: the Bounty/Issue/Branch instances loaded earlier in this
        // session are now stale (their rows were removed above via bulk SQL, bypassing
        // Hibernate), and must not still be in the persistence context when deleteById()
        // does its own entity-level removal.
        order.verify(entityManager).clear();
        order.verify(codebaseRepository).deleteById(10L);
    }

    @Test
    void deleteRepositoryRecords_ShouldDeletePullRequestsBeforeIssues() {
        when(codebaseService.findById(10L)).thenReturn(codebase);

        deletionService.deleteRepositoryRecords(10L);

        // pull_request.id (FK to issues.id) and its branch references have no ON DELETE rule,
        // so a PR row left behind would block deleting either its parent issue or the branch
        // it points to.
        InOrder order = inOrder(issueRepository);
        order.verify(issueRepository).deletePullRequestsByRepositoryId(10L);
        order.verify(issueRepository).deleteIssuesByRepositoryId(10L);
    }

    @Test
    void deleteRepositoryRecords_ShouldRemoveBranchRows() {
        when(codebaseService.findById(10L)).thenReturn(codebase);

        deletionService.deleteRepositoryRecords(10L);

        verify(branchService).deleteAllBranchesForCodebase(codebase);
    }

    @Test
    void deleteRepositoryRecords_ShouldThrowNotFound_WhenRepositoryMissing() {
        when(codebaseService.findById(999L)).thenThrow(new CodebaseNotFoundException("Repository not found: Id = 999"));

        assertThatThrownBy(() -> deletionService.deleteRepositoryRecords(999L))
                .isInstanceOf(CodebaseNotFoundException.class);

        verifyNoInteractions(bountyService, transactionService, issueRepository, branchService, codebaseRepository, entityManager);
    }
}
