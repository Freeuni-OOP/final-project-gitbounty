package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.model.Branch;

public record BranchResponse(
    Long id,
    String name,
    Long latestCommitId
) {
    public static BranchResponse from(Branch branch) {
        // Strip the prefix here so the API consumer only sees the short name
        String displayName = branch.getName();
        if (displayName != null && displayName.startsWith("refs/heads/")) {
            displayName = displayName.substring("refs/heads/".length());
        }

        return new BranchResponse(
            branch.getId(),
            displayName,
            branch.getLatestCommit() != null ? branch.getLatestCommit().getId() : null
        );
    }
}