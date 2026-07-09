package org.gitbounty.gitbountybackend.service.codebase;

import jakarta.persistence.EntityManager;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the DB-side cascade for deleting a repository. Kept separate from
 * CodebaseService/BountyService/IssueService because those three already depend on
 * each other (CodebaseService -> ... -> IssueService -> CodebaseService), so wiring
 * this cascade into any one of them would create a circular bean dependency.
 *
 * Filesystem cleanup is intentionally NOT done here - the caller runs it after this
 * transaction has committed, so a failed disk delete never rolls back the DB state.
 */
@Service
public class CodebaseDeletionService {

    private final CodebaseService codebaseService;
    private final CodebaseRepository codebaseRepository;
    private final BountyService bountyService;
    private final TransactionService transactionService;
    private final IssueRepository issueRepository;
    private final BranchService branchService;
    private final EntityManager entityManager;

    public CodebaseDeletionService(CodebaseService codebaseService,
                                    CodebaseRepository codebaseRepository,
                                    BountyService bountyService,
                                    TransactionService transactionService,
                                    IssueRepository issueRepository,
                                    BranchService branchService,
                                    EntityManager entityManager) {
        this.codebaseService = codebaseService;
        this.codebaseRepository = codebaseRepository;
        this.bountyService = bountyService;
        this.transactionService = transactionService;
        this.issueRepository = issueRepository;
        this.branchService = branchService;
        this.entityManager = entityManager;
    }

    /**
     * Deletes every DB row owned by a repository, in FK-safe order, in one transaction:
     * refund active bounties, detach transaction audit rows from those bounties (including
     * the refunds just recorded), delete issues (cascades pull requests and bounties),
     * delete branches, then the codebase row itself.
     *
     * Issues, pull requests, and branches are removed via bulk JPQL deletes
     * (Query.executeUpdate()), not by loading entities and calling EntityManager.remove().
     * Entity-level removal across this object graph (Transaction -> Bounty -> Issue ->
     * Codebase, plus the joined-table PullRequest/Issue inheritance) makes Hibernate's
     * flush-time "transient reference" check reason about several interrelated entities
     * disappearing in the same flush - which is exactly what previously threw
     * TransientPropertyValueException. Bulk deletes bypass that check entirely: they run as
     * plain SQL against the matching rows, independent of what the persistence context has
     * loaded.
     *
     * Does not touch the filesystem; the caller is responsible for deleting the bare git
     * repository directory after this method returns successfully.
     */
    @Transactional
    public Codebase deleteRepositoryRecords(Long repositoryId) {
        Codebase codebase = codebaseService.findById(repositoryId);
        Long codebaseId = codebase.getId();

        bountyService.cancelActiveBountiesForRepository(codebaseId);
        transactionService.detachBountyReferencesForRepository(codebaseId);

        // Flush so the refund/detach changes above reach the database before the bulk
        // deletes run: the detach must land first, or deleting a bounty while a
        // transaction still references it (in the DB) would violate that foreign key.
        entityManager.flush();

        // Pull requests first: pull_request.id (FK to issues.id) and its branch references
        // have no ON DELETE rule, so a PR row left behind would block deleting either its
        // parent issue or the branch it points to.
        issueRepository.deletePullRequestsByRepositoryId(codebaseId);
        issueRepository.deleteIssuesByRepositoryId(codebaseId); // cascades bounties at the DB level (ON DELETE CASCADE)
        branchService.deleteAllBranchesForCodebase(codebase);

        // Clear so the Bounty/Issue/Branch instances loaded earlier in this session (now
        // stale, since the bulk deletes above removed their rows without going through
        // Hibernate) aren't still sitting in the persistence context when deleteById()
        // below does its own entity-level removal.
        entityManager.clear();

        codebaseRepository.deleteById(codebaseId);

        return codebase;
    }
}
