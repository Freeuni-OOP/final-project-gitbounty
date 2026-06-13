package org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto;

import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.model.PullRequest;

public record ListPullRequestResponse(
    Long id,
    Integer number,
    String title,
    String description,
    String sourceBranch,
    String targetBranch,
    IssueStatus status,
    String authorUsername
) {
    public static ListPullRequestResponse from(PullRequest pr) {
        return new ListPullRequestResponse(
            pr.getId(),
            pr.getNumber(),
            pr.getTitle(),
            pr.getDescription(),
            pr.getSourceBranch().getName(),
            pr.getTargetBranch().getName(),
            pr.getStatus(),
            pr.getAuthor().getUsername()
        );
    }
}
