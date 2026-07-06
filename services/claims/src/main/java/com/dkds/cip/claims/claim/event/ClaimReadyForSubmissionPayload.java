package com.dkds.cip.claims.claim.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimReadyForSubmissionPayload(
        UUID claimId,
        UUID clinicId,
        UUID petId,
        UUID policyId,
        String decision,
        BigDecimal approvedAmount,
        String origin,
        UUID sourceSessionId,
        Instant updatedAt
) {
}
