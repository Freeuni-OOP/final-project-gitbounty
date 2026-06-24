package org.gitbounty.gitbountybackend.controller.payment.dto;

import java.math.BigDecimal;

public record CreateCreditTopUpRequest(
    BigDecimal amountPaid,
    BigDecimal creditsToPurchase,
    String cardholderName,
    String cardNumber,
    Integer expiryMonth,
    Integer expiryYear,
    String cvv
) {
}
