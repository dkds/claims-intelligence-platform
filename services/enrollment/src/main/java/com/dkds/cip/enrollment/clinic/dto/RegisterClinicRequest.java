package com.dkds.cip.enrollment.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterClinicRequest(
        @NotBlank String name,
        String addressLine1,
        String addressLine2,
        String city,
        String postcode,
        String countryCode,
        String contactEmail,
        String contactPhone
) {
}
