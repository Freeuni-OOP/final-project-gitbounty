package org.gitbounty.gitbountybackend.controller.issue;

import java.time.LocalDateTime;

import org.gitbounty.gitbountybackend.model.Issue;

public record IssueResponse(
        Long id,
        Integer number,
        String title,
        String description,
        String status,
        Long authorId,
        String authorUsername,
        Long assignedToId,
        String assignedToUsername,
        Long repositoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getId(),
                issue.getNumber(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus().name(),
                issue.getAuthor().getId(),
                issue.getAuthor().getUsername(),
                issue.getAssignedTo() == null ? null : issue.getAssignedTo().getId(),
                issue.getAssignedTo() == null ? null : issue.getAssignedTo().getUsername(),
                issue.getRepository().getId(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}