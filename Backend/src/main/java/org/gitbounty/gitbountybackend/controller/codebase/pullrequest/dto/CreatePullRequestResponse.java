package org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto;

import org.gitbounty.gitbountybackend.model.PullRequest;

import java.time.LocalDateTime;

public record CreatePullRequestResponse(
    Long id,
    Integer number,
    String title,
    String description,
    String sourceBranch,
    String targetBranch,
    String status,
    String authorUsername,
    LocalDateTime createdAt
) {
    public static CreatePullRequestResponse from(PullRequest pr) {
        return new CreatePullRequestResponse(
            pr.getId(),
            pr.getNumber(),
            pr.getTitle(),
            pr.getDescription(),
            pr.getSourceBranch().getName(),
            pr.getTargetBranch().getName(),
            pr.getStatus().name(),
            pr.getAuthor().getUsername(),
            pr.getCreatedAt()
        );
    }
}