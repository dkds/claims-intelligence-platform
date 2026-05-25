package com.dkds.cip.enrollment.policy.dto;

import com.dkds.cip.enrollment.policy.CoverageType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignPolicyRequest(
        @NotNull CoverageType coverageType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
