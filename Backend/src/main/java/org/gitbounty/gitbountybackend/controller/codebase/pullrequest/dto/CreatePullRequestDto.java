package org.gitbounty.gitbountybackend.controller.codebase.pullrequest.dto;

public record CreatePullRequestDto(
    String sourceBranch,
    String targetBranch,
    String title,
    String description
) {}