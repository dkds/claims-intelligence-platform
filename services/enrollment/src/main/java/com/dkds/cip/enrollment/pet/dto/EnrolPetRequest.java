package com.dkds.cip.enrollment.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record EnrolPetRequest(
        @NotNull UUID ownerId,
        @NotBlank String name,
        @NotBlank String species,
        String breed,
        LocalDate dateOfBirth,
        String microchipNumber
) {
}
