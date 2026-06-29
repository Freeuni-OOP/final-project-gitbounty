package org.gitbounty.gitbountybackend.controller.codebase.pullrequest;

import org.gitbounty.gitbountybackend.controller.codebase.CodebasePermissions;
import org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto.CreatePullRequestDto;
import org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto.CreatePullRequestResponse;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.CreatePullRequestCommand;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
                jwt.getSubject(),
                dto.sourceBranch(),
                dto.targetBranch(),
                dto.title(),
                dto.description()
        );
        return CreatePullRequestResponse.from(pullRequestService.createPullRequest(createPullRequestCommand));
    }

    @GetMapping("/{prNumber}")
    @ResponseStatus(HttpStatus.OK)
    public CreatePullRequestResponse getPullRequest(
            @PathVariable String repositoryName,
            @PathVariable Integer prNumber
    ) {
        return CreatePullRequestResponse.from(pullRequestService.getPullRequest(repositoryName, prNumber));
    }

    @PostMapping("/{prNumber}/merge")
    @ResponseStatus(HttpStatus.OK)
    public void mergePullRequest(
            @PathVariable String repositoryName,
            @PathVariable Integer prNumber,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Only the codebase owner can merge pull requests.");
        }
        pullRequestService.mergePullRequestForCodebase(repositoryName, prNumber);
    }

    @PatchMapping("/{prNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closePullRequest(
            @PathVariable String repositoryName,
            @PathVariable Integer prNumber,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Only the codebase owner can change pull requests.");
        }
        pullRequestService.updatePRStatus(repositoryName, prNumber, IssueStatus.CLOSED);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CreatePullRequestResponse> findAllPullRequests(@PathVariable String repositoryName) {
        return pullRequestService.getPullRequestsForCodebase(repositoryName).stream()
                .map(CreatePullRequestResponse::from)
                .toList();
    }
}