package org.gitbounty.gitbountybackend.service.codebase.issue.pullRequest;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final BranchRepository branchRepository;
    private final UserService userService;
    private final IssueRepository issueRepository;
    private final CodebaseRepository codebaseRepository;

    PullRequestService(
            PullRequestRepository pullRequestRepository,
            BranchRepository branchRepository,
            UserService userService,
            IssueRepository issueRepository,
            CodebaseRepository codebaseRepository
    ) {
        this.pullRequestRepository = pullRequestRepository;
        this.branchRepository = branchRepository;
        this.userService = userService;
        this.issueRepository = issueRepository;
        this.codebaseRepository = codebaseRepository;
    }

    @Transactional
    public PullRequest createPullRequest(Long codebaseId, Long userId, String sourceBranchName, String targetBranchName,
                                         String title, String description) {
        String normalizedTitle = PullRequest.normalizeTitle(title);

        User author = resolveUser(userId);
        Codebase codebase = resolveCodebase(codebaseId);
        Branch sourceBranch = resolveBranch(codebaseId, sourceBranchName);
        Branch targetBranch = resolveBranch(codebaseId, targetBranchName);

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

    /**
     * Resolves a user by ID.
     * @param userId the user ID
     * @return the User
     * @throws ResponseStatusException if userId is null or user not found
     */
    private User resolveUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        return userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: id=" + userId));
    }

    /**
     * Resolves a codebase by ID.
     * @param codebaseId the codebase ID
     * @return the Codebase
     * @throws ResponseStatusException if codebaseId is null or codebase not found
     */
    private Codebase resolveCodebase(Long codebaseId) {
        if (codebaseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codebase id is required");
        }
        return codebaseRepository.findById(codebaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Codebase not found: id=" + codebaseId));
    }

    /**
     * Resolves the source branch by name and codebase ID.
     * @param codebaseId the codebase ID
     * @param branchName the source branch name
     * @return the source Branch, or null if sourceBranchName is null/blank
     * @throws ResponseStatusException if source branch not found
     */
    private Branch resolveBranch(Long codebaseId, String branchName) {
        if(branchName == null || branchName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch name is required");
        }
        return branchRepository.findByCodebaseIdAndName(codebaseId, branchName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "branch not found: " + branchName));
    }

}
