package org.gitbounty.gitbountybackend.controller.issue;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.gitbounty.gitbountybackend.controller.codebase.CodebasePermissions;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/codebases/{repositoryName}/issues")
public class IssueController {

    private final IssueService issueService;
    private final CodebasePermissions codebasePermissions;

    public IssueController(
            IssueService issueService,
            CodebasePermissions codebasePermissions
    ) {
        this.issueService = issueService;
        this.codebasePermissions = codebasePermissions;
    }

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @PathVariable String repositoryName,
            @RequestBody CreateIssueRequest request,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Issue issue = issueService.createIssue(
                repositoryName,
                request.title(),
                request.description(),
                principal
        );

        return ResponseEntity
                .created(URI.create("/api/codebases/" + repositoryName + "/issues/" + issue.getId()))
                .body(IssueResponse.from(issue));
    }

    @GetMapping
    public ResponseEntity<List<IssueResponse>> listIssues(
            @PathVariable String repositoryName
    ) {
        List<IssueResponse> issues = issueService.listIssues(repositoryName)
                .stream()
                .map(IssueResponse::from)
                .toList();

        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{issueNumber}")
    public ResponseEntity<IssueResponse> getIssue(
            @PathVariable String repositoryName,
            @PathVariable Integer issueNumber
    ) {
        Issue issue = issueService.getIssue(repositoryName, issueNumber);

        return ResponseEntity.ok(IssueResponse.from(issue));
    }

    @PatchMapping("/{issueNumber}/state")
    public ResponseEntity<IssueResponse> updateIssueState(
            @PathVariable String repositoryName,
            @PathVariable Integer issueNumber,
            @RequestBody UpdateIssueStateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null || !codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Don't have permission to update issue state");
        }

        Issue issue = issueService.updateIssueState(
                repositoryName,
                issueNumber,
                request.status()
        );

        return ResponseEntity.ok(IssueResponse.from(issue));
    }
}
