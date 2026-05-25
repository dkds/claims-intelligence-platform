package com.dkds.cip.enrollment.vet.dto;

public record UpdateVetRequest(
        String firstName,
        String lastName,
        String email,
        String licenseNumber
) {
}
