package com.dkds.cip.claims.claim.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApproveClaimRequest(
        @NotNull UUID approvedBy
) {
}
