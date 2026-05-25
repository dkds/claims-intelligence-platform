package com.dkds.cip.enrollment.catalogue.dto;

import com.dkds.cip.enrollment.catalogue.CatalogueItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogueItemResponse(
        UUID id,
        String code,
        String description,
        BigDecimal reimbursementRate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CatalogueItemResponse from(CatalogueItem item) {
        return new CatalogueItemResponse(
                item.getId(),
                item.getCode(),
                item.getDescription(),
                item.getReimbursementRate(),
                item.isActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
