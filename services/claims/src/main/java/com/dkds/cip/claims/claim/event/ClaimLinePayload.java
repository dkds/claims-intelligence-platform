package com.dkds.cip.claims.claim.event;

import java.math.BigDecimal;

public record ClaimLinePayload(String procedureCode, int quantity, BigDecimal requestedAmount) {
}
