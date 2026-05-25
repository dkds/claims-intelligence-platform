package com.dkds.cip.enrollment.clinic.dto;

import com.dkds.cip.enrollment.clinic.Clinic;
import com.dkds.cip.enrollment.clinic.ClinicStatus;

import java.time.Instant;
import java.util.UUID;

public record ClinicResponse(
        UUID id,
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String postcode,
        String countryCode,
        String contactEmail,
        String contactPhone,
        ClinicStatus status,
        Instant registeredAt,
        Instant updatedAt
) {
    public static ClinicResponse from(Clinic c) {
        return new ClinicResponse(
                c.getId(),
                c.getName(),
                c.getAddressLine1(),
                c.getAddressLine2(),
                c.getCity(),
                c.getPostcode(),
                c.getCountryCode(),
                c.getContactEmail(),
                c.getContactPhone(),
                c.getStatus(),
                c.getRegisteredAt(),
                c.getUpdatedAt()
        );
    }
}
