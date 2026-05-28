package org.gitbounty.gitbountybackend.controller.Codebase;

import java.time.LocalDateTime;

import org.gitbounty.gitbountybackend.model.Codebase;

public record CodebaseResponse(
    Long id,
    String name,
    String description,
    String gitUrl,
    String ownerUsername,
    LocalDateTime createdAt
) {
    public static CodebaseResponse from(Codebase codebase) {
        return new CodebaseResponse(
            codebase.getId(),
            codebase.getName(),
            codebase.getDescription(),
            codebase.getGitUrl(),
            codebase.getOwner() != null ? codebase.getOwner().getUsername() : null,
            codebase.getCreatedAt()
        );
    }
}

