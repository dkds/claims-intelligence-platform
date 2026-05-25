package com.dkds.cip.enrollment.owner.dto;

import com.dkds.cip.enrollment.owner.Owner;

import java.time.Instant;
import java.util.UUID;

public record OwnerResponse(
        UUID id,
        UUID clinicId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Instant createdAt
) {
    public static OwnerResponse from(Owner o) {
        return new OwnerResponse(
                o.getId(),
                o.getClinicId(),
                o.getFirstName(),
                o.getLastName(),
                o.getEmail(),
                o.getPhone(),
                o.getCreatedAt()
        );
    }
}
