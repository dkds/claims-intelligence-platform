package com.dkds.cip.claims.claim.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClaimRoutedToReviewPayload(
        UUID claimId,
        List<String> reasons,
        String routedBy,
        String origin,
        Instant updatedAt
) {
}
