package org.gitbounty.gitbountybackend.controller.Issue;

public record CreateIssueRequest(
        String title,
        String description
) {
}