package org.gitbounty.gitbountybackend.controller.Codebase.pullRequest;

import org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto.CreatePullRequestDto;
import org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto.PullRequestResponse;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullRequest.PullRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/codebases/{repositoryName}/pull-requests")
class PullRequestController {
    private final PullRequestService pullRequestService;

    public PullRequestController(PullRequestService pullRequestService) {
        this.pullRequestService = pullRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PullRequestResponse createPullRequest(
        @PathVariable String repositoryName,
        @RequestBody CreatePullRequestDto dto,
        @AuthenticationPrincipal Jwt jwt) {

        // Assuming your service resolves the repository by name/ID and creates the PR
        return PullRequestResponse.from(
            pullRequestService.createPullRequest(
            repositoryName,
            jwt.getSubject(), // user ID from JWT
            dto.sourceBranch(),
            dto.targetBranch(),
            dto.title(),
            dto.description()
        ));
    }
}
