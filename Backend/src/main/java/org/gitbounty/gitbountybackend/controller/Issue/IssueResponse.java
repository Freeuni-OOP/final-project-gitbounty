package org.gitbounty.gitbountybackend.controller.Issue;

import java.time.LocalDateTime;

import org.gitbounty.gitbountybackend.model.Issue;

public record IssueResponse(
        Long id,
        String title,
        String description,
        String status,
        Long authorId,
        Long repositoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus().name(),
                issue.getAuthor().getId(),
                issue.getRepository().getId(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}