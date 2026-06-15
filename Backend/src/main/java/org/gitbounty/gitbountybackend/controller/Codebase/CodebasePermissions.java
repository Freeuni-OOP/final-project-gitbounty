package org.gitbounty.gitbountybackend.controller.Codebase;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.springframework.stereotype.Component;

/**
 * Centralized permission methods for codebase method-security annotations.
 */
@Component("codebasePermissions")
public class CodebasePermissions {

    private final CodebaseService codebaseService;

    public CodebasePermissions(CodebaseService codebaseService) {
        this.codebaseService = codebaseService;
    }

    /**
     * Check whether the authenticated user is the owner of the given codebase.
     * Only the owner is allowed to manage members.
     */
    public boolean isOwner(String repositoryName, String authenticatedUsername) {
        if (repositoryName == null || repositoryName.isBlank()
                || authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return false;
        }

        Codebase codebase = codebaseService.getCodebase(repositoryName);
        return authenticatedUsername.equals(codebase.getOwner().getUsername());
    }
}