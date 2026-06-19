package org.gitbounty.gitbountybackend.controller.transaction.dto;

import java.math.BigDecimal;

public record CreditBalanceResponse(Long userId, BigDecimal creditBalance) {
}