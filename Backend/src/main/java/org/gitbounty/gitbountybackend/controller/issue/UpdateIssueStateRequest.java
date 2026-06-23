package org.gitbounty.gitbountybackend.controller.issue;

import org.gitbounty.gitbountybackend.model.IssueStatus;

public record UpdateIssueStateRequest(
        IssueStatus status
) {
}