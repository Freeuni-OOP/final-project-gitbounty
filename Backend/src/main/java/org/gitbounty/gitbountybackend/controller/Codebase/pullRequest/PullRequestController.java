package org.gitbounty.gitbountybackend.controller.Codebase.pullRequest;

import org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto.CreatePullRequestDto;
import org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto.CreatePullRequestResponse;
import org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto.ListPullRequestResponse;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullRequest.PullRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/codebases/{repositoryName}/pull-requests")
class PullRequestController {
    private final PullRequestService pullRequestService;

    public PullRequestController(PullRequestService pullRequestService) {
        this.pullRequestService = pullRequestService;
    }

    @PostMapping
    public CreatePullRequestResponse createPullRequest(
        @PathVariable String repositoryName,
        @RequestBody CreatePullRequestDto dto,
        @AuthenticationPrincipal Jwt jwt) {

        // Assuming your service resolves the repository by name/ID and creates the PR
        return CreatePullRequestResponse.from(
            pullRequestService.createPullRequest(
            repositoryName,
            jwt.getSubject(), // user ID from JWT
            dto.sourceBranch(),
            dto.targetBranch(),
            dto.title(),
            dto.description()
        ));
    }

    // API endpoint for listing all issues for a repository
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ListPullRequestResponse> findAllPullRequests(
        @PathVariable String repositoryName,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return pullRequestService.getPullRequestsForCodebase(repositoryName).stream()
            .map(ListPullRequestResponse::from)
            .toList();
    }


}
