package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

public record CreatePullRequestCommand(
    String codebaseName,
    String userId,
    String sourceBranchName, // normal name like "feature" or "main"
    String targetBranchName,
    String title,
    String description
) {
}
