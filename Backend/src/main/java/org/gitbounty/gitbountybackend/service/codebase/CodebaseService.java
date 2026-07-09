package org.gitbounty.gitbountybackend.service.codebase;

import jakarta.persistence.EntityManager;
import java.util.List;

import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.service.codebase.dto.CodebaseContentsDTO;
import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.dto.UpdateCodebaseCommand;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestRepository;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.model.Branch;

@Service
public class CodebaseService {

    private static final Logger log = LoggerFactory.getLogger(CodebaseService.class);

    private final CodebaseRepository codebaseRepository;
    private final CodebaseStorageService codebaseStorageService;
    private final UserService userService;
    private final BranchService branchService;
    private final TransactionService transactionService;
    private final IssueRepository issueRepository;
    private final PullRequestRepository pullRequestRepository;
    private final EntityManager entityManager;
    private final CodebaseService self;

    public CodebaseService(CodebaseRepository codebaseRepository,
                            CodebaseStorageService codebaseStorageService,
                            UserService userService,
                            BranchService branchService,
                            TransactionService transactionService,
                            IssueRepository issueRepository,
                            PullRequestRepository pullRequestRepository,
                            EntityManager entityManager,
                            @Lazy CodebaseService self) {
        this.codebaseRepository = codebaseRepository;
        this.codebaseStorageService = codebaseStorageService;
        this.userService = userService;
        this.branchService = branchService;
        this.transactionService = transactionService;
        this.issueRepository = issueRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.entityManager = entityManager;
        this.self = self;
    }

    public Codebase createCodebase(String name, String description, String gitUrl, String userId) {
        String repositoryName = normalize(name);
        User owner = userService.findByKeycloakId(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (codebaseRepository.findByName(repositoryName).isPresent()) {
            throw new IllegalStateException("Repository already exists");
        }

        codebaseStorageService.createRepository(repositoryName);

        try {
            Codebase codebase = codebaseRepository.saveAndFlush(new Codebase(repositoryName, description == null ? null : description.trim(), gitUrl, owner));
            branchService.createNewBranchForCodebase(codebase, Branch.DEFAULT_NAME);

            return codebase;
        } catch (RuntimeException e) {
            try { codebaseStorageService.deleteRepository(repositoryName); }
            catch (RuntimeException cleanupError) { e.addSuppressed(cleanupError); }
            throw e;
        }
    }

    public Codebase getCodebase(String repositoryName) {
        String normalized = normalize(repositoryName);
        return codebaseRepository.findByName(normalized).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: " + normalized));
    }

    public Codebase findById(Long repositoryId) {
        return codebaseRepository.findById(repositoryId).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: Id = " + repositoryId));
    }

    public CodebaseContentsDTO listCodebaseContents(String repositoryName, String path, String branchName) {
        if (!codebaseRepository.existsByName(repositoryName)) {
            throw new CodebaseNotFoundException("Codebase not found: " + repositoryName);
        }
        return new CodebaseContentsDTO(codebaseStorageService.getPathContents(repositoryName, path, branchName));
    }

    public void deleteCodebase(String name) {
        String repositoryName = normalize(name);
        codebaseRepository.findByName(repositoryName).ifPresent(codebaseRepository::delete);
        codebaseStorageService.deleteRepository(repositoryName);
    }

    public Codebase findByName(String repositoryName) {
        return codebaseRepository.findByName(repositoryName).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: " + repositoryName));
    }

    public List<Codebase> getAllCodebases() {
        return codebaseRepository.findAll();
    }

    public Codebase updateCodebase(String repositoryName, UpdateCodebaseCommand command) {
        Codebase codebase = getCodebase(repositoryName); //reuses the getCodebase logic to avoid duplicating the throw
        command.applyTo(codebase);
        return codebaseRepository.save(codebase);
    }

    /**
     * Deletes a repository entirely: the DB cascade (transactions detached, pull requests,
     * issues, branches, and the codebase row itself) in one transaction, then the bare git
     * repository directory on disk strictly after that transaction commits.
     *
     * This is two methods rather than one because calling deleteRepositoryRecords via
     * "this." would bypass Spring's transactional proxy entirely (the well-known
     * self-invocation pitfall), silently breaking the DB cascade's atomicity - each
     * repository call inside it would commit on its own instead of all-or-nothing. Routing
     * the call through the @Lazy self-reference goes through the real proxy, so the DB step
     * stays one real transaction, while the filesystem step - deliberately outside that
     * transaction - only runs after it commits, and never rolls the DB back if it fails.
     */
    public Codebase deleteRepository(Long repositoryId) {
        Codebase deleted = self.deleteRepositoryRecords(repositoryId);

        try {
            codebaseStorageService.deleteRepository(deleted.getName());
        } catch (RuntimeException e) {
            // The DB deletion already committed; an orphaned directory is recoverable
            // and must not fail the request or trigger a rollback.
            log.warn("Failed to delete repository directory for '{}'", deleted.getName(), e);
        }

        return deleted;
    }

    /**
     * Deletes every DB row owned by a repository, in FK-safe order, in one transaction:
     * detach pre-existing transactions from this repo's bounties, delete pull requests,
     * delete issues (each issue's bounty, if still active, is refunded automatically by
     * BountyPreRemoveListener as it cascades away), delete branches, then the codebase row.
     *
     * Row removal here is entity-level (repository.delete(...) per row) rather than bulk
     * JPQL/native SQL. Bulk deletes bypass Hibernate's persistence context and its
     * @PreRemove callbacks entirely, which is exactly what caused this cascade's two prior
     * bugs: a stale Transaction/Bounty reference throwing TransientPropertyValueException,
     * and a JOINED-inheritance bulk delete requiring a Hibernate-managed temp table that
     * doesn't exist under this Flyway-managed (ddl-auto=none) schema. Entity-level removal
     * sidesteps both - normal managed-entity dirty-checking keeps everything consistent,
     * and @PreRemove fires exactly like it would for any other delete.
     *
     * Does not touch the filesystem; deleteRepository above handles that after this
     * transaction has committed.
     */
    @Transactional
    public Codebase deleteRepositoryRecords(Long repositoryId) {
        Codebase codebase = findById(repositoryId);
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

    private String normalize(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Repository name is required");
        }
        String trimmed = name.trim();
        if (trimmed.matches(".*[/\\\\].*") || trimmed.contains("..") || trimmed.contains(".git")) {
            throw new IllegalArgumentException("Invalid repository name format");
        }
        return trimmed;
    }
}
