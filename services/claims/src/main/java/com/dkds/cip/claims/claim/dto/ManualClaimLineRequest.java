package com.dkds.cip.claims.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ManualClaimLineRequest(
        @NotBlank String procedureCode,
        @Positive int quantity,
        @Positive BigDecimal requestedAmount
) {
}
