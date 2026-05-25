package com.dkds.cip.enrollment.pet.dto;

import com.dkds.cip.enrollment.pet.Pet;
import com.dkds.cip.enrollment.pet.PetStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PetResponse(
        UUID id,
        UUID clinicId,
        UUID ownerId,
        String name,
        String species,
        String breed,
        LocalDate dateOfBirth,
        String microchipNumber,
        PetStatus status,
        Instant enrolledAt
) {
    public static PetResponse from(Pet p) {
        return new PetResponse(
                p.getId(),
                p.getClinicId(),
                p.getOwnerId(),
                p.getName(),
                p.getSpecies(),
                p.getBreed(),
                p.getDateOfBirth(),
                p.getMicrochipNumber(),
                p.getStatus(),
                p.getEnrolledAt()
        );
    }
}
