package com.dkds.cip.enrollment.vet.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectVetRequest(@NotBlank String reason) {
}
