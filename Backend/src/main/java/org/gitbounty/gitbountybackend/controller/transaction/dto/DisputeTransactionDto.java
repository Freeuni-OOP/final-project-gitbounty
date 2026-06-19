package org.gitbounty.gitbountybackend.controller.transaction.dto;

public record DisputeTransactionDto(
    Long transactionId,
    Long disputantId,
    String reason
) {}

