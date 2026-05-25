package com.dkds.cip.enrollment.clinic.dto;

public record UpdateClinicRequest(
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String postcode,
        String countryCode,
        String contactEmail,
        String contactPhone
) {
}
