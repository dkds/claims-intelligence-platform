package com.dkds.cip.claims.claim.dto;

import com.dkds.cip.claims.claim.ClaimLine;

import java.math.BigDecimal;
import java.util.UUID;

public record ClaimLineResponse(
        UUID id,
        String procedureCode,
        int quantity,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount
) {
    public static ClaimLineResponse from(ClaimLine line) {
        return new ClaimLineResponse(
                line.getId(),
                line.getProcedureCode(),
                line.getQuantity(),
                line.getRequestedAmount(),
                line.getApprovedAmount()
        );
    }
}
