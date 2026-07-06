package com.dkds.cip.claims.claim.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClaimAssembledPayload(
        UUID claimId,
        UUID clinicId,
        UUID petId,
        UUID policyId,
        String origin,
        UUID sourceSessionId,
        UUID submittedBy,
        List<ClaimLinePayload> lines,
        BigDecimal totalRequested,
        Instant assembledAt
) {
}
