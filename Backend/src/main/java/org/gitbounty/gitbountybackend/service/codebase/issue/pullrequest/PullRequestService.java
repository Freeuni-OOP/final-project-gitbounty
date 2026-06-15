package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitbounty.gitbountybackend.exception.BranchNotFoundException;
import org.gitbounty.gitbountybackend.exception.PRBranchesAreSameException;
import org.gitbounty.gitbountybackend.exception.ResourceNotFoundException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.git.GitService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;


@Service
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final BranchRepository branchRepository;
    private final UserService userService;
    private final IssueRepository issueRepository;
    private final CodebaseService codebaseService;
    private final GitService gitService;

    PullRequestService(
            PullRequestRepository pullRequestRepository,
            BranchRepository branchRepository,
            UserService userService,
            IssueRepository issueRepository,
            CodebaseService codebaseService,
            GitService gitService
    ) {
        this.pullRequestRepository = pullRequestRepository;
        this.branchRepository = branchRepository;
        this.userService = userService;
        this.issueRepository = issueRepository;
        this.codebaseService = codebaseService;
        this.gitService = gitService;
    }

    public PullRequest createPullRequest(CreatePullRequestCommand request) {
        User author = resolveUser(request.userId());
        Codebase codebase = resolveCodebase(request.codebaseName());

        Branch sourceBranch = resolveBranch(codebase.getId(), request.sourceBranchName());
        Branch targetBranch = resolveBranch(codebase.getId(), request.targetBranchName());

        if (sourceBranch.equals(targetBranch)) {
            throw new PRBranchesAreSameException("Source and target branches cannot be the same");
        }

        Integer nextNumber = issueRepository.findMaxNumberByRepositoryId(codebase.getId())
            .map(maxNumber -> maxNumber + 1)
            .orElse(1);

        PullRequest pr = new PullRequest();
        pr.setTitle(PullRequest.normalizeTitle(request.title()));
        pr.setDescription(PullRequest.normalizeDescription(request.description()));
        pr.setNumber(nextNumber);
        pr.setAuthor(author);
        pr.setRepository(codebase);
        pr.setSourceBranch(sourceBranch);
        pr.setTargetBranch(targetBranch);

        return pullRequestRepository.saveAndFlush(pr);
    }

    public List<PullRequest> getPullRequestsForCodebase(String repositoryName) {
        Codebase codebase = codebaseService.findByName(repositoryName);
        return pullRequestRepository.findByRepository(codebase);
    }

    @Transactional
    public PullRequest mergePullRequestForCodebase(String repositoryName, Integer prNumber) throws IOException {
        Codebase codebase = codebaseService.findByName(repositoryName);
        PullRequest pr = pullRequestRepository.findByRepositoryAndNumber(codebase, prNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Pull request not found: " + prNumber + " For repository: " + repositoryName));

        try {
            // Assuming your PR stores source/target branch names
            MergeResult result = gitService.mergeBranches(
                codebase.getName(),
                pr.getSourceBranch().getName(),
                pr.getTargetBranch().getName()
            );

            // 3. Update status based on merge result
            if (result.getMergeStatus().isSuccessful()) {
                pr.setStatus(IssueStatus.CLOSED);
                pr.setMergedAt(Instant.now());
            } else if (result.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING) {
                // Optionally save conflict details to the PR entity
            }


            return pullRequestRepository.save(pr);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resolves a user by ID.
     * @param userId the user ID
     * @return the User
     * @throws ResponseStatusException if userId is null or user not found
     */
    private User resolveUser(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return userService.findByKeycloakId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: id=" + userId));
    }

    /**
     * Resolves a codebase by Name.
     * @param codebaseName the codebase name
     * @return the Codebase
     * @throws ResponseStatusException if codebaseName is null or codebase not found
     */
    private Codebase resolveCodebase(String codebaseName) {
        if (codebaseName == null || codebaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Codebase Name is required");
        }
        return codebaseService.findByName(codebaseName);
    }

    /**
     * Resolves the source branch by name and codebase ID.
     * @param codebaseId the codebase ID
     * @param branchName the source branch name
     * @return the source Branch, or null if sourceBranchName is null/blank
     * @throws ResponseStatusException if source branch not found
     */
    private Branch resolveBranch(Long codebaseId, String branchName) {
        if(branchName == null || branchName.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name is required");
        }
        return branchRepository.findByCodebaseIdAndName(codebaseId, branchName)
                .orElseThrow(() -> new BranchNotFoundException("branch not found: " + branchName));
    }

}
