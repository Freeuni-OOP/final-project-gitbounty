package org.gitbounty.gitbountybackend.controller.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateCreditTopUpRequest(
    @NotNull(message = "Credits to purchase is required")
    @Positive(message = "Credits to purchase must be greater than 0")
    @DecimalMax(value = "1000", message = "Credits to purchase must not exceed 1000 per transaction")
    BigDecimal creditsToPurchase,
    @NotBlank(message = "Cardholder name is required")
    String cardholderName,
    @NotBlank(message = "Card number is required")
    String cardNumber,
    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    Integer expiryMonth,
    @NotNull(message = "Expiry year is required")
    Integer expiryYear,
    @NotBlank(message = "CVV is required")
    String cvv,
    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey
) {
}
