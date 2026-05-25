package com.dkds.cip.enrollment.vet.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterVetRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String email,
        @NotBlank String licenseNumber
) {
}
