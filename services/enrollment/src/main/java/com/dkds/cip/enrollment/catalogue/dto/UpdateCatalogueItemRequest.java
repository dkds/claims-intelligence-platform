package com.dkds.cip.enrollment.catalogue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateCatalogueItemRequest(
        @NotBlank String description,
        @DecimalMin("0.00") BigDecimal reimbursementRate
) {
}
