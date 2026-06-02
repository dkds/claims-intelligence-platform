package com.dkds.cip.claims.claim.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ClaimAdjudicatedPayload(
        UUID claimId,
        String decision,
        BigDecimal approvedAmount,
        List<String> reasons,
        String adjudicatedBy,
        String origin,
        UUID sourceSessionId
) {
}
