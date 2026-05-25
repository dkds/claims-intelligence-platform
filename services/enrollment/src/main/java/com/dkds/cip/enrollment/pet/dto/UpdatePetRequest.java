package com.dkds.cip.enrollment.pet.dto;

import java.time.LocalDate;

public record UpdatePetRequest(
        String name,
        String species,
        String breed,
        LocalDate dateOfBirth,
        String microchipNumber
) {
}
