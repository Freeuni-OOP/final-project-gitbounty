package org.gitbounty.gitbountybackend.controller.codebase.pullrequest;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitbounty.gitbountybackend.controller.codebase.CodebasePermissions;
import org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto.CreatePullRequestDto;
import org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto.CreatePullRequestResponse;
import org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto.PullRequestDiffResponse;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.CreatePullRequestCommand;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/codebases/{repositoryName}/pull-requests")
class PullRequestController {
    private final PullRequestService pullRequestService;
    private final CodebasePermissions codebasePermissions;

    PullRequestController(PullRequestService pullRequestService, CodebasePermissions codebasePermissions) {
        this.pullRequestService = pullRequestService;
        this.codebasePermissions = codebasePermissions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePullRequestResponse createPullRequest(
        @PathVariable String repositoryName,
        @RequestBody CreatePullRequestDto dto,
        @AuthenticationPrincipal Jwt jwt) {

        CreatePullRequestCommand createPullRequestCommand = new CreatePullRequestCommand(
            repositoryName,
            jwt.getSubject(), // user ID from JWT
            dto.sourceBranch(),
            dto.targetBranch(),
            dto.title(),
            dto.description()
        );
        // Assuming your service resolves the repository by name/ID and creates the PR
        return CreatePullRequestResponse.from(
            pullRequestService.createPullRequest(createPullRequestCommand));
    }
    // Get a specific Pull Request by number
    @GetMapping("/{prNumber}")
    @ResponseStatus(HttpStatus.OK)
    public CreatePullRequestResponse getPullRequest(
        @PathVariable String repositoryName,
        @PathVariable Integer prNumber
    ) {
        return CreatePullRequestResponse.from(
            pullRequestService.getPullRequest(repositoryName, prNumber)
        );
    }
    // Get the raw unified diff text for a specific Pull Request
    @GetMapping("/{prNumber}/diff")
    @ResponseStatus(HttpStatus.OK)
    public PullRequestDiffResponse getPullRequestDiff(
        @PathVariable String repositoryName,
        @PathVariable Integer prNumber
    ) throws IOException {
        String rawDiff = pullRequestService.getPullRequestDiff(repositoryName, prNumber);
        return new PullRequestDiffResponse(repositoryName, prNumber, rawDiff);
    }

    // Merge a Pull Request
    @PostMapping("/{prNumber}/merge")
    @ResponseStatus(HttpStatus.OK)
    public void mergePullRequest(
        @PathVariable String repositoryName,
        @PathVariable Integer prNumber,
        @AuthenticationPrincipal Jwt jwt
    ) throws GitAPIException, IOException {
        if(!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Only the codebase owner can merge pull requests.");
        }
        pullRequestService.mergePullRequestForCodebase(repositoryName, prNumber, jwt.getSubject());
    }

    // Delete/Close a Pull Request
    @PatchMapping("/{prNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closePullRequest(
        @PathVariable String repositoryName,
        @PathVariable Integer prNumber,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if(!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Only the codebase owner can change pull requests.");
        }
        pullRequestService.updatePRStatus(repositoryName, prNumber, IssueStatus.CLOSED);
    }
    // API endpoint for listing all issues for a repository
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CreatePullRequestResponse> findAllPullRequests(
        @PathVariable String repositoryName
    ) {
        return pullRequestService.getPullRequestsForCodebase(repositoryName).stream()
            .map(CreatePullRequestResponse::from)
            .toList();
    }


}
