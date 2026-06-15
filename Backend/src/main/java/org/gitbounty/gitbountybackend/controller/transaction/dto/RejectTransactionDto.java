package org.gitbounty.gitbountybackend.controller.transaction.dto;

public record RejectTransactionDto(
    Long transactionId,
    Long rejecterId,
    String reason
) {}

