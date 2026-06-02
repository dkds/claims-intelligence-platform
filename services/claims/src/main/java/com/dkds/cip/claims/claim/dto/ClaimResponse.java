package com.dkds.cip.claims.claim.dto;

import com.dkds.cip.claims.claim.Claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        UUID clinicId,
        UUID petId,
        UUID policyId,
        String origin,
        UUID sourceSessionId,
        UUID submittedBy,
        String status,
        String adjudicationDecision,
        BigDecimal totalRequested,
        BigDecimal approvedAmount,
        Instant createdAt,
        List<ClaimLineResponse> lines
) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getClinicId(),
                claim.getPetId(),
                claim.getPolicyId(),
                claim.getOrigin().name(),
                claim.getSourceSessionId(),
                claim.getSubmittedBy(),
                claim.getStatus().name(),
                claim.getAdjudicationDecision() != null ? claim.getAdjudicationDecision().name() : null,
                claim.getTotalRequested(),
                claim.getApprovedAmount(),
                claim.getCreatedAt(),
                claim.getLines().stream().map(ClaimLineResponse::from).toList()
        );
    }
}
