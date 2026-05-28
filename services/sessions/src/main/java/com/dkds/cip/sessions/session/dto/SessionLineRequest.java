package com.dkds.cip.sessions.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SessionLineRequest(
        @NotBlank String procedureCode,
        @Min(1) int quantity,
        String notes
) {
}
