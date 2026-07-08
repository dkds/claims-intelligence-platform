package com.dkds.cip.claims.claim.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RejectClaimRequest(
        @NotNull UUID rejectedBy,
        String reason
) {
}
