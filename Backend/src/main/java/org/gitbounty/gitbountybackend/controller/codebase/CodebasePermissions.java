package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberRepository;
import org.springframework.stereotype.Component;

/**
 * Centralized permission methods for codebase method-security annotations.
 */
@Component("codebasePermissions")
public class CodebasePermissions {

    private final CodebaseService codebaseService;
    private final CodebaseMemberRepository codebaseMemberRepository;

    public CodebasePermissions(CodebaseService codebaseService,
                              CodebaseMemberRepository codebaseMemberRepository) {
        this.codebaseService = codebaseService;
        this.codebaseMemberRepository = codebaseMemberRepository;
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

    public boolean isOwnerBySubject(String repositoryName, String subject) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        return codebase.getOwner().getKeycloakId().equals(subject);
    }

    /**
     * Check whether the authenticated user can push to the given codebase.
     * Allows both owners and members with DEVELOPER or MAINTAINER role.
     */
    public boolean canPush(String repositoryName, String authenticatedUsername) {
        if (repositoryName == null || repositoryName.isBlank()
                || authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return false;
        }

        Codebase codebase = codebaseService.getCodebase(repositoryName);
        
        // Owner can always push
        if (authenticatedUsername.equals(codebase.getOwner().getUsername())) {
            return true;
        }

        // Check if user is a member with developer or maintainer access
        return codebaseMemberRepository.findByCodebaseId(codebase.getId())
            .stream()
            .anyMatch(member -> member.getUser().getUsername().equals(authenticatedUsername) &&
                    (member.getRole() == CodebaseRole.MAINTAINER || member.getRole() == CodebaseRole.DEVELOPER));
    }
}