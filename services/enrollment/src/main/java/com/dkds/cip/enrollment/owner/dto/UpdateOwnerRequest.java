package com.dkds.cip.enrollment.owner.dto;

public record UpdateOwnerRequest(
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
