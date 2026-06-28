package org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto;

public record PullRequestDiffResponse(
    String repositoryName,
    Integer prNumber,
    String rawDiff
) {}