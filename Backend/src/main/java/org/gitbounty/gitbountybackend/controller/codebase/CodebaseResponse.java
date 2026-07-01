package org.gitbounty.gitbountybackend.controller.codebase;

import java.time.LocalDateTime;
import java.util.List;

import org.gitbounty.gitbountybackend.model.Codebase;

public record CodebaseResponse(
    Long id,
    String name,
    String description,
    String gitUrl,
    String ownerUsername,
    LocalDateTime createdAt,
    List<BranchResponse> branches
) {
    public static CodebaseResponse from(Codebase codebase) {
        return new CodebaseResponse(
            codebase.getId(),
            codebase.getName(),
            codebase.getDescription(),
            codebase.getGitUrl(),
            codebase.getOwner() != null ? codebase.getOwner().getUsername() : null,
            codebase.getCreatedAt(),
            List.of()
        );
    }

    public static CodebaseResponse from(Codebase codebase, List<BranchResponse> branches) {
        return new CodebaseResponse(
            codebase.getId(),
            codebase.getName(),
            codebase.getDescription(),
            codebase.getGitUrl(),
            codebase.getOwner() != null ? codebase.getOwner().getUsername() : null,
            codebase.getCreatedAt(),
            branches
        );
    }
}

