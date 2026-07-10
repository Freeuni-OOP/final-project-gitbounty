package org.gitbounty.gitbountybackend.controller.codebase;

import java.util.EnumSet;
import java.util.Set;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.stereotype.Component;

/**
 * Centralized permission methods for codebase method-security annotations.
 */
@Component("codebasePermissions")
public class CodebasePermissions {

    // REPORTER is a read-only collaborator; everyone else can write to the repository.
    private static final Set<CodebaseRole> WRITE_ROLES =
            EnumSet.of(CodebaseRole.OWNER, CodebaseRole.MAINTAINER, CodebaseRole.DEVELOPER);

    private final CodebaseService codebaseService;
    private final CodebaseMemberRepository codebaseMemberRepository;
    private final UserService userService;

    public CodebasePermissions(CodebaseService codebaseService,
                                CodebaseMemberRepository codebaseMemberRepository,
                                UserService userService) {
        this.codebaseService = codebaseService;
        this.codebaseMemberRepository = codebaseMemberRepository;
        this.userService = userService;
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
     * Checks whether the authenticated user may delete the repository: either the
     * owner, or a codebase member with a write/admin role (not a read-only REPORTER).
     */
    public boolean canDeleteRepository(Long repositoryId, String subject) {
        if (repositoryId == null || subject == null || subject.isBlank()) {
            return false;
        }

        Codebase codebase = codebaseService.findById(repositoryId);

        if (codebase.getOwner() != null && subject.equals(codebase.getOwner().getKeycloakId())) {
            return true;
        }

        return userService.findByKeycloakId(subject)
                .flatMap(user -> codebaseMemberRepository.findByCodebaseIdAndUserId(codebase.getId(), user.getId()))
                .map(member -> WRITE_ROLES.contains(member.getRole()))
                .orElse(false);
    }
}