package org.gitbounty.gitbountybackend.controller.transaction.dto;

import org.gitbounty.gitbountybackend.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    Long fromUserId,
    Long toUserId,
    Long issueId,
    BigDecimal amount,
    String status,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime resolvedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getFromUser().getId(),
            transaction.getToUser().getId(),
                transaction.getBounty() != null && transaction.getBounty().getIssue() != null
                        ? transaction.getBounty().getIssue().getId()
                        : null,
            transaction.getAmount(),
            transaction.getStatus().name(),
            transaction.getDescription(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt(),
            transaction.getResolvedAt()
        );
    }
}

