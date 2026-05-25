package com.dkds.cip.enrollment.catalogue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateCatalogueItemRequest(
        @NotBlank String code,
        @NotBlank String description,
        @NotNull @DecimalMin("0.00") BigDecimal reimbursementRate
) {
}
