package org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto;

import org.gitbounty.gitbountybackend.model.PullRequest;

import java.time.LocalDateTime;

public record PullRequestResponse(
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
    public static PullRequestResponse from(PullRequest pr) {
        return new PullRequestResponse(
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