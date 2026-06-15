package org.gitbounty.gitbountybackend.controller.issue;

public record CreateIssueRequest(
        String title,
        String description
) {
}