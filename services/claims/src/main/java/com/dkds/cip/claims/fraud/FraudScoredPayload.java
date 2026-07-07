package com.dkds.cip.claims.fraud;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudScoredPayload(
        UUID claimId,
        double score,
        String riskLevel,
        List<String> flags,
        String modelVersion,
        Instant scoredAt
) {
}
