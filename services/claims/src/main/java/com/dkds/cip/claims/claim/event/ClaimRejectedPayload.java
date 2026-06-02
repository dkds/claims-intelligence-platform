package com.dkds.cip.claims.claim.event;

import java.util.List;
import java.util.UUID;

public record ClaimRejectedPayload(
        UUID claimId,
        List<String> reasons,
        String rejectedBy,
        String origin
) {
}
