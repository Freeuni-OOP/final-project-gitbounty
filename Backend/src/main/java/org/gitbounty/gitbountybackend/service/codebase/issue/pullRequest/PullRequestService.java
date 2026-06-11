package org.gitbounty.gitbountybackend.service.codebase.issue.pullRequest;

import org.gitbounty.gitbountybackend.exception.BranchNotFoundException;
import org.gitbounty.gitbountybackend.exception.PRBranchesAreSameException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final BranchRepository branchRepository;
    private final UserService userService;
    private final IssueRepository issueRepository;
    private final CodebaseService codebaseService;

    PullRequestService(
            PullRequestRepository pullRequestRepository,
            BranchRepository branchRepository,
            UserService userService,
            IssueRepository issueRepository,
            CodebaseService codebaseService
    ) {
        this.pullRequestRepository = pullRequestRepository;
        this.branchRepository = branchRepository;
        this.userService = userService;
        this.issueRepository = issueRepository;
        this.codebaseService = codebaseService;
    }

    @Transactional
    public PullRequest createPullRequest(Codebase codebase, User author, Branch sourceBranch, Branch targetBranch, String title, String description){
        String normalizedTitle = PullRequest.normalizeTitle(title);

        if(sourceBranch.equals(targetBranch)) {
            throw new PRBranchesAreSameException("Source and target branches shouldn't be the same");
        }
        // compute next issue number within the repository
        Integer nextNumber = issueRepository.findMaxNumberByRepositoryId(codebase.getId())
            .map(maxNumber -> maxNumber + 1)
            .orElse(1);

        PullRequest pr = new PullRequest();
        pr.setTitle(normalizedTitle);
        pr.setDescription(PullRequest.normalizeDescription(description));
        pr.setNumber(nextNumber);
        pr.setAuthor(author);
        pr.setRepository(codebase);
        pr.setSourceBranch(sourceBranch);
        pr.setTargetBranch(targetBranch);
        return pullRequestRepository.saveAndFlush(pr);

    }
    @Transactional
    public PullRequest createPullRequest(Long codebaseId, String userId, String sourceBranchName, String targetBranchName,
                                         String title, String description) {
        User author = resolveUser(userId);
        Codebase codebase = resolveCodebase(codebaseId);
        Branch sourceBranch = resolveBranch(codebaseId, sourceBranchName);
        Branch targetBranch = resolveBranch(codebaseId, targetBranchName);

        return createPullRequest(codebase, author, sourceBranch, targetBranch, title, description);
    }

    // Could use a rewrite using command pattern but i'd need to change tests for which there isn't time
    @Transactional
    public PullRequest createPullRequest(String repositoryName, String userId, String sourceBranchName, String targetBranchName,
                                         String title, String description) {
        Codebase codebase = codebaseService.findByName(repositoryName);
        return createPullRequest(codebase.getId(), userId, sourceBranchName, targetBranchName, title, description);
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
     * Resolves a codebase by ID.
     * @param codebaseId the codebase ID
     * @return the Codebase
     * @throws ResponseStatusException if codebaseId is null or codebase not found
     */
    private Codebase resolveCodebase(Long codebaseId) {
        if (codebaseId == null) {
            throw new IllegalArgumentException("Codebase id is required");
        }
        return codebaseService.findById(codebaseId);
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
