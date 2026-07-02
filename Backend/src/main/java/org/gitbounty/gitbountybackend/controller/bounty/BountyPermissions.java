package org.gitbounty.gitbountybackend.controller.bounty;

import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.springframework.stereotype.Component;

@Component("bountyPermissions")
public class BountyPermissions {

    private final IssueRepository issueRepository;
    private final BountyRepository bountyRepository;

    public BountyPermissions(IssueRepository issueRepository, BountyRepository bountyRepository) {
        this.issueRepository = issueRepository;
        this.bountyRepository = bountyRepository;
    }

    /**
     * Checks repository ownership using issue ID.
     */
    public boolean isIssueRepositoryOwner(Long issueId, String keycloakUserId) {
        if (isInvalid(issueId, keycloakUserId)) {
            return false;
        }

        return issueRepository.findById(issueId).map(issue ->
                isRepositoryOwner(issue, keycloakUserId)).orElse(false);
    }

    /**
     * Checks repository ownership using bounty ID.
     */
    public boolean isBountyRepositoryOwner(Long bountyId, String keycloakUserId) {
        if (isInvalid(bountyId, keycloakUserId)) {
            return false;
        }

        return bountyRepository.findById(bountyId).map(bounty
                -> isRepositoryOwner(bounty.getIssue(), keycloakUserId)).orElse(false);
    }

    private boolean isRepositoryOwner(Issue issue, String keycloakUserId) {
        if (issue == null || issue.getRepository() == null || issue.getRepository().getOwner() == null) {
            return false;
        }

        String ownerId = issue.getRepository().getOwner().getKeycloakId();
        return ownerId != null && ownerId.equals(keycloakUserId);
    }

    private boolean isInvalid(Long resourceId, String keycloakUserId) {
        return resourceId == null || keycloakUserId == null || keycloakUserId.isBlank();
    }
}