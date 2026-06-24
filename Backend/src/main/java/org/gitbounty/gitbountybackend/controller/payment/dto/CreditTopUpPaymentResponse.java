package org.gitbounty.gitbountybackend.controller.payment.dto;

import org.gitbounty.gitbountybackend.model.CreditTopUpPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditTopUpPaymentResponse(
    Long id,
    Long userId,
    BigDecimal amountPaid,
    BigDecimal creditsGranted,
    String cardBrand,
    String cardLast4,
    String status,
    LocalDateTime createdAt
) {
    public static CreditTopUpPaymentResponse from(CreditTopUpPayment payment) {
        return new CreditTopUpPaymentResponse(
            payment.getId(),
            payment.getUser().getId(),
            payment.getAmountPaid(),
            payment.getCreditsGranted(),
            payment.getCardBrand(),
            payment.getCardLast4(),
            payment.getStatus().name(),
            payment.getCreatedAt()
        );
    }
}
