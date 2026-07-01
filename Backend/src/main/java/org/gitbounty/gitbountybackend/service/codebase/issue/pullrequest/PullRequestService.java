package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
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

import org.gitbounty.gitbountybackend.service.codebase.issue.event.IssueClosedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final BranchRepository branchRepository;
    private final UserService userService;
    private final IssueRepository issueRepository;
    private final CodebaseService codebaseService;
    private final GitService gitService;
    private final PullRequestPersistenceService persistenceService;
    private final ApplicationEventPublisher eventPublisher;

    PullRequestService(
        PullRequestRepository pullRequestRepository,
        BranchRepository branchRepository,
        UserService userService,
        IssueRepository issueRepository,
        CodebaseService codebaseService,
        GitService gitService,
        PullRequestPersistenceService persistenceService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.pullRequestRepository = pullRequestRepository;
        this.branchRepository = branchRepository;
        this.userService = userService;
        this.issueRepository = issueRepository;
        this.codebaseService = codebaseService;
        this.gitService = gitService;
        this.persistenceService = persistenceService;
        this.eventPublisher = eventPublisher;
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

        return persistenceService.create(request, author, codebase, source, target, nextNumber);
    }

    public List<PullRequest> getPullRequestsForCodebase(String repositoryName) {
        Codebase codebase = codebaseService.findByName(repositoryName);
        return pullRequestRepository.findByRepository(codebase);
    }

    /**
     * Merges the PR and announces its closure for bounty handling.
     */
    @Transactional
    public void mergePullRequestForCodebase(String repositoryName, Integer prNumber, String userId) throws IOException, GitAPIException {
        Codebase codebase = codebaseService.getCodebase(repositoryName);

        PullRequest pr = pullRequestRepository.findByRepositoryAndNumber(codebase, prNumber)
                .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));

        User mergerUser = userService.findByKeycloakId(userId)
                .orElseThrow(() -> new UserNotFoundException("User executing merge not found."));

        PersonIdent mergeIdentity = new PersonIdent(mergerUser.getUsername(), mergerUser.getEmail());

        gitService.runLocked(repositoryName, () -> {
            MergeResult result = gitService.mergeBranches(
                    repositoryName,
                    pr.getSourceBranch().getName(),
                    pr.getTargetBranch().getName(),
                    mergeIdentity
            );

            try {
                PullRequest mergedPullRequest = persistenceService.finalizeMerge(pr.getId());
                eventPublisher.publishEvent(IssueClosedEvent.completed(pr.getId(), pr.getAuthor().getId()));

                return mergedPullRequest;
            } catch (Exception exception) {
                gitService.revertMerge(repositoryName, pr.getTargetBranch().getName(), result.getNewHead(), mergeIdentity);

                throw new DatabaseTransactionException("Database update failed, " + "Git state rolled back.", exception);
            }
        });
    }

    public PullRequest getPullRequest(String repositoryName, Integer prNumber) {
        return pullRequestRepository.findByRepositoryAndNumber(
                codebaseService.findByName(repositoryName), prNumber)
            .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));
    }

    /**
     * Change a PR state and announces closure.
     */
    @Transactional
    public void updatePRStatus(String repositoryName, Integer prNumber, IssueStatus issueStatus) {
        Codebase codebase = codebaseService.findByName(repositoryName);
        PullRequest pr = pullRequestRepository.findByRepositoryAndNumber(codebase, prNumber)
                .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));

        persistenceService.updatePRStatus(pr.getId(), issueStatus);

        if (issueStatus == IssueStatus.CLOSED) {
            eventPublisher.publishEvent(IssueClosedEvent.cancelled(pr.getId()));
        }
    }

    public String getPullRequestDiff(String repositoryName, Integer prNumber) throws IOException {
        Codebase codebase = codebaseService.findByName(repositoryName);
        PullRequest pr = pullRequestRepository.findByRepositoryAndNumber(codebase, prNumber)
            .orElseThrow(() -> new PRNotFoundException(prNumber, repositoryName));

        return gitService.getBranchDiff(
            repositoryName,
            pr.getSourceBranch().getName(),
            pr.getTargetBranch().getName()
        );
    }
}