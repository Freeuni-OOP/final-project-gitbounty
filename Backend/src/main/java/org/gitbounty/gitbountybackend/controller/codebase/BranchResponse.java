package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.model.Branch;

public record BranchResponse(
        Long id,
        String name,
        Long latestCommitId
) {
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getLatestCommit() != null ? branch.getLatestCommit().getId() : null
        );
    }
}

