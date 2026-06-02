package com.dkds.cip.claims.adjudication;

import java.math.BigDecimal;
import java.util.List;

public record AdjudicationResult(
        AdjudicationDecision decision,
        BigDecimal totalApproved,
        List<String> reasons
) {
    public static AdjudicationResult approved(BigDecimal amount) {
        return new AdjudicationResult(AdjudicationDecision.APPROVED, amount, List.of());
    }

    public static AdjudicationResult partiallyApproved(BigDecimal amount, List<String> reasons) {
        return new AdjudicationResult(AdjudicationDecision.PARTIALLY_APPROVED, amount, reasons);
    }

    public static AdjudicationResult rejected(List<String> reasons) {
        return new AdjudicationResult(AdjudicationDecision.REJECTED, BigDecimal.ZERO, reasons);
    }
}
