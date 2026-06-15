package org.gitbounty.gitbountybackend.controller.transaction.dto;

public record CreateTransactionDto(
    Long fromUserId,
    Long toUserId,
    Long issueId
) {}

