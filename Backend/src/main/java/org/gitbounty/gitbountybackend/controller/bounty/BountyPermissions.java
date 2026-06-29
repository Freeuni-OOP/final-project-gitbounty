package org.gitbounty.gitbountybackend.controller.bounty;

import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.springframework.stereotype.Component;

@Component("bountyPermissions")
public class BountyPermissions {

    private final IssueRepository issueRepository;

    public BountyPermissions(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    /**
     * Checks if the authenticated ID matches the repository owners ID
     */
    public boolean isIssueRepositoryOwner(Long issueId, String keycloakUserId) {
        if (issueId == null || keycloakUserId == null || keycloakUserId.isBlank()) return false;

        return issueRepository.findById(issueId).map(issue -> {
                    String ownerId = issue.getRepository().getOwner().getKeycloakId();
                    return ownerId != null && ownerId.equals(keycloakUserId);
                }).orElse(false);
    }
}