package com.dkds.cip.enrollment.vet.dto;

import com.dkds.cip.enrollment.vet.Vet;
import com.dkds.cip.enrollment.vet.VetStatus;

import java.time.Instant;
import java.util.UUID;

public record VetResponse(
        UUID id,
        UUID clinicId,
        String firstName,
        String lastName,
        String email,
        String licenseNumber,
        VetStatus status,
        String rejectionReason,
        Instant registeredAt,
        Instant updatedAt
) {
    public static VetResponse from(Vet v) {
        return new VetResponse(
                v.getId(),
                v.getClinicId(),
                v.getFirstName(),
                v.getLastName(),
                v.getEmail(),
                v.getLicenseNumber(),
                v.getStatus(),
                v.getRejectionReason(),
                v.getRegisteredAt(),
                v.getUpdatedAt()
        );
    }
}
