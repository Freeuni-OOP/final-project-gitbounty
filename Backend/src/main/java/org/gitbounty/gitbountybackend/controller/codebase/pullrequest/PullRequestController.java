package org.gitbounty.gitbountybackend.controller.codebase.pullrequest;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto.CreatePullRequestDto;
import org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto.CreatePullRequestResponse;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.CreatePullRequestCommand;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/codebases/{repositoryName}/pull-requests")
class PullRequestController {
    private final PullRequestService pullRequestService;

    public PullRequestController(PullRequestService pullRequestService) {
        this.pullRequestService = pullRequestService;
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

    // Merge a Pull Request
    @PostMapping("/{prNumber}/merge")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@codebasePermissions.isOwnerBySubject(#repositoryName, #jwt.subject)")
    public void mergePullRequest(
        @PathVariable String repositoryName,
        @PathVariable Integer prNumber,
        @AuthenticationPrincipal Jwt jwt
    ) throws GitAPIException, IOException {
        pullRequestService.mergePullRequestForCodebase(repositoryName, prNumber);
    }

    // Delete/Close a Pull Request
    @DeleteMapping("/{prNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@codebasePermissions.isOwnerBySubject(#repositoryName, #jwt.subject)")
    public void deletePullRequest(
        @PathVariable String repositoryName,
        @PathVariable Integer prNumber,
        @AuthenticationPrincipal Jwt jwt
    ) {
        pullRequestService.deletePullRequestForCodebase(repositoryName, prNumber);
    }
    // Delete/Close a Pull Request
    @PatchMapping("/{prNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@codebasePermissions.isOwnerBySubject(#repositoryName, #jwt.subject)")
    public void closePullRequest(
        @PathVariable String repositoryName,
        @PathVariable Integer prNumber,
        @AuthenticationPrincipal Jwt jwt
    ) {
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
