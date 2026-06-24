package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitbounty.gitbountybackend.exception.*;
import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.git.GitService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final BranchRepository branchRepository;
    private final UserService userService;
    private final IssueRepository issueRepository;
    private final CodebaseService codebaseService;
    private final GitService gitService;
    private final PullRequestPersistenceService persistenceService;

    PullRequestService(
        PullRequestRepository pullRequestRepository,
        BranchRepository branchRepository,
        UserService userService,
        IssueRepository issueRepository,
        CodebaseService codebaseService,
        GitService gitService, PullRequestPersistenceService persistenceService
    ) {
        this.pullRequestRepository = pullRequestRepository;
        this.branchRepository = branchRepository;
        this.userService = userService;
        this.issueRepository = issueRepository;
        this.codebaseService = codebaseService;
        this.gitService = gitService;
        this.persistenceService = persistenceService;
    }

    public PullRequest createPullRequest(CreatePullRequestCommand request) {
        User author = userService.findByKeycloakId(request.userId())
            .orElseThrow(() -> new UserNotFoundException("User not found: id=" + request.userId()));
        Codebase codebase = codebaseService.getCodebase(request.codebaseName());
        Branch source = branchRepository.findByCodebaseIdAndName(codebase.getId(), request.sourceBranchName())
            .orElseThrow(() ->  new BranchNotFoundException("Branch not found " + request.sourceBranchName()));
        Branch target = branchRepository.findByCodebaseIdAndName(codebase.getId(), request.targetBranchName())
            .orElseThrow(() ->  new BranchNotFoundException("Branch not found " + request.targetBranchName()));

        if (source.equals(target)) {
            throw new PRBranchesAreSameException("Source and target branches cannot be the same");
        }

        Integer nextNumber = issueRepository.findMaxNumberByRepositoryId(codebase.getId())
            .map(n -> n + 1).orElse(1);

        // Delegation to Persistence Service
        return persistenceService.create(request, author, codebase, source, target, nextNumber);
    }

    public List<PullRequest> getPullRequestsForCodebase(String repositoryName) {
        Codebase codebase = codebaseService.findByName(repositoryName);
        return pullRequestRepository.findByRepository(codebase);
    }

    public void mergePullRequestForCodebase(String repositoryName, Integer prNumber) throws IOException, GitAPIException {
        Codebase codebase = codebaseService.findByName(repositoryName);
        PullRequest pr = pullRequestRepository.findByRepositoryAndNumber(codebase, prNumber)
            .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));

        // Execute the Git operation inside the locked scope
        gitService.runLocked(repositoryName, () -> {
            MergeResult result = gitService.mergeBranches(
                repositoryName,
                pr.getSourceBranch().getName(),
                pr.getTargetBranch().getName()
            );

            try {
                // Update DB while inside the lock
                return persistenceService.finalizeMerge(pr.getId());
            } catch (Exception e) {
                // ROLLBACK: Revert the specific commit we just pushed
                gitService.revertMerge(repositoryName, result.getNewHead());
                throw new DatabaseTransactionException("Database update failed, Git state rolled back.", e);
            }
        });
    }

    public void deletePullRequestForCodebase(String repositoryName, Integer prNumber) {
        Codebase codebase = codebaseService.findByName(repositoryName);
        PullRequest pr = pullRequestRepository.findByRepositoryAndNumber(codebase, prNumber)
            .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));

        persistenceService.delete(pr.getId());
    }

    public PullRequest getPullRequest(String repositoryName, Integer prNumber) {
        return pullRequestRepository.findByRepositoryAndNumber(
            codebaseService.findByName(repositoryName), prNumber)
            .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));
    }

    public void updatePRStatus(String repositoryName, Integer prNumber, IssueStatus issueStatus) {
        Codebase codebase = codebaseService.findByName(repositoryName);
        PullRequest pr = pullRequestRepository.findByRepositoryAndNumber(codebase, prNumber)
            .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));

        persistenceService.updatePRStatus(pr.getId(), issueStatus);
    }
}
