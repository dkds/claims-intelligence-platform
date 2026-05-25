package com.dkds.cip.enrollment.owner.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterOwnerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String email,
        String phone
) {
}
