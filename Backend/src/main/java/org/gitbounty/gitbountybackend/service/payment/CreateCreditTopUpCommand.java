package org.gitbounty.gitbountybackend.service.payment;

import java.math.BigDecimal;

public record CreateCreditTopUpCommand(
    BigDecimal creditsToPurchase,
    String cardholderName,
    String cardNumber,
    Integer expiryMonth,
    Integer expiryYear,
    String cvv,
    String idempotencyKey
) {
}