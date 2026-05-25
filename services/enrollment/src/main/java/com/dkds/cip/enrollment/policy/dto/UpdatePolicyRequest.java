package com.dkds.cip.enrollment.policy.dto;

import com.dkds.cip.enrollment.policy.PolicyStatus;

import java.time.LocalDate;

public record UpdatePolicyRequest(
        LocalDate endDate,
        PolicyStatus status
) {
}
