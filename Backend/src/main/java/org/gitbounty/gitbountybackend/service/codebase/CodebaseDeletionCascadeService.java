package org.gitbounty.gitbountybackend.service.codebase;

import jakarta.persistence.EntityManager;

import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns exactly the transactional DB cascade for repository deletion, as its own bean,
 * deliberately separate from CodebaseService.
 *
 * This class exists so CodebaseService.deleteRepository can reach the @Transactional
 * cascade below through a genuinely different bean's Spring proxy. Calling an @Transactional
 * method via "this." (self-invocation) bypasses the proxy entirely, silently breaking the
 * cascade's atomicity - each repository call inside it would commit on its own instead of
 * all-or-nothing. An earlier version of this fix used a @Lazy self-injected CodebaseService
 * reference to route through CodebaseService's own proxy; reviewer feedback flagged that as
 * unnecessary complexity, since routing the call through a distinct bean achieves the same
 * result without any self-reference.
 *
 * Looks up the Codebase via CodebaseRepository directly rather than depending on
 * CodebaseService for it: CodebaseService.deleteRepository calls this class, and
 * cancelIfActive(...) below reaches BountyService -> IssueService -> CodebaseService, so a
 * CodebaseService dependency here would recreate a bean cycle
 * (CodebaseService -> this -> BountyService -> IssueService -> CodebaseService). BountyService
 * is injected @Lazy specifically to break that cycle at this end of the chain - the same
 * lazy-proxy technique Spring recommends generally for circular bean dependencies, distinct
 * from the self-invocation anti-pattern this class was extracted to eliminate.
 */
@Service
public class CodebaseDeletionCascadeService {

    private final CodebaseRepository codebaseRepository;
    private final BranchService branchService;
    private final TransactionService transactionService;
    private final IssueRepository issueRepository;
    private final PullRequestRepository pullRequestRepository;
    private final EntityManager entityManager;
    private final BountyService bountyService;

    public CodebaseDeletionCascadeService(CodebaseRepository codebaseRepository,
                                           BranchService branchService,
                                           TransactionService transactionService,
                                           IssueRepository issueRepository,
                                           PullRequestRepository pullRequestRepository,
                                           EntityManager entityManager,
                                           @Lazy BountyService bountyService) {
        this.codebaseRepository = codebaseRepository;
        this.branchService = branchService;
        this.transactionService = transactionService;
        this.issueRepository = issueRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.entityManager = entityManager;
        this.bountyService = bountyService;
    }

    /**
     * Deletes every DB row owned by a repository, in FK-safe order, in one transaction:
     * detach pre-existing transactions from this repo's bounties, delete pull requests,
     * then for each issue - cancel and refund its bounty first if still active, then
     * delete the issue itself (its bounty cascades away with it) - delete branches, then
     * the codebase row.
     *
     * Bounty cancellation is an explicit call scoped to each individual issue, immediately
     * before that issue is deleted, rather than one upfront "cancel every bounty in the
     * repository" pass before this loop starts. An upfront pass is exactly what caused
     * this cascade's first bug: refund Transactions it created sat stale in the
     * persistence context by the time the later per-entity deletes ran, throwing
     * TransientPropertyValueException. Scoping cancellation to the same iteration as its
     * issue's deletion keeps each bounty's full lifecycle - refund, then delete - inside
     * one self-contained step.
     *
     * Row removal here is entity-level (repository.delete(...) per row) rather than bulk
     * JPQL/native SQL: a bulk delete against the JOINED-inheritance Issue/PullRequest
     * hierarchy requires a Hibernate-managed temp table that doesn't exist under this
     * Flyway-managed (ddl-auto=none) schema - this was this cascade's second prior bug.
     *
     * Does not touch the filesystem; CodebaseService.deleteRepository handles that after
     * this transaction has committed.
     */
    @Transactional
    public Codebase deleteRepositoryRecords(Long repositoryId) {
        Codebase codebase = codebaseRepository.findById(repositoryId)
                .orElseThrow(() -> new CodebaseNotFoundException("Repository not found: Id = " + repositoryId));
        Long codebaseId = codebase.getId();

        transactionService.detachBountyReferencesForRepository(codebaseId);
        // Flushed and cleared between every phase below on purpose: Hibernate's pre-flush
        // "transient reference" check walks every managed entity in the persistence context,
        // including ones already removed in this same flush. With several interrelated
        // entities (Issue -> Codebase, Transaction -> Bounty) all disappearing in the same
        // transaction, that check can trip over an entity that's mid-removal and misreport
        // it as a transient reference - this held true even after switching every deletion
        // below from a bulk query to per-entity removal. Flushing lets Hibernate fully
        // execute and settle one phase's changes; clearing then drops those now-removed
        // entities from the persistence context so the next phase's flush never has to
        // reason about two connected entities disappearing at once. Codebase becomes
        // detached after the first clear(), so only its already-loaded id/name (captured
        // above) are used afterward, never the entity itself.
        entityManager.flush();
        entityManager.clear();

        // Pull requests first: pull_request.id (FK to issues.id) and its branch references
        // have no ON DELETE rule, so a PR row left behind would block deleting either its
        // parent issue or the branch it points to.
        for (PullRequest pullRequest : pullRequestRepository.findByRepository(codebase)) {
            pullRequestRepository.delete(pullRequest);
        }
        for (Issue issue : issueRepository.findByRepositoryId(codebaseId)) {
            bountyService.cancelIfActive(issue.getBounty());
            issueRepository.delete(issue);
        }
        entityManager.flush();
        entityManager.clear();

        branchService.deleteAllBranchesForCodebase(codebase);
        entityManager.flush();
        entityManager.clear();

        codebaseRepository.deleteById(codebaseId);

        return codebase;
    }
}
