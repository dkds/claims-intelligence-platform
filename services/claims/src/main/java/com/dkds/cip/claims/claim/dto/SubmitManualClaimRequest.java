package com.dkds.cip.claims.claim.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitManualClaimRequest(
        @NotNull UUID petId,
        @NotNull UUID policyId,
        @NotNull UUID submittedBy,
        @NotEmpty @Valid List<ManualClaimLineRequest> lines
) {
}
