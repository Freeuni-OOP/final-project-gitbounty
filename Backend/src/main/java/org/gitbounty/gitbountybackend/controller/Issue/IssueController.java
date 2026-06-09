package org.gitbounty.gitbountybackend.controller.Issue;

import java.net.URI;
import java.security.Principal;

import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.service.issue.IssueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/codebases/{repositoryName}/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @PathVariable String repositoryName,
            @RequestBody CreateIssueRequest request,
            Principal principal
    ) {
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
}