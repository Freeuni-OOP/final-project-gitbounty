package org.gitbounty.gitbountybackend.controller.transaction.dto;

public record ApproveTransactionDto(
    Long transactionId,
    Long approverId
) {}

