package com.dkds.cip.enrollment.policy.dto;

import com.dkds.cip.enrollment.policy.CoverageType;
import com.dkds.cip.enrollment.policy.Policy;
import com.dkds.cip.enrollment.policy.PolicyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        UUID petId,
        CoverageType coverageType,
        LocalDate startDate,
        LocalDate endDate,
        PolicyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PolicyResponse from(Policy p) {
        return new PolicyResponse(
                p.getId(),
                p.getPetId(),
                p.getCoverageType(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
