package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

public record CreatePullRequestCommand(
    String codebaseName,
    String userId,
    String sourceBranchName,
    String targetBranchName,
    String title,
    String description
) {
}
