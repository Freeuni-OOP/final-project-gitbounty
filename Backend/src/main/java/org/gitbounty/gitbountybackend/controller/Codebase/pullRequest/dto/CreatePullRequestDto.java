package org.gitbounty.gitbountybackend.controller.Codebase.pullRequest.dto;

public record CreatePullRequestDto(
    String sourceBranch,
    String targetBranch,
    String title,
    String description
) {}