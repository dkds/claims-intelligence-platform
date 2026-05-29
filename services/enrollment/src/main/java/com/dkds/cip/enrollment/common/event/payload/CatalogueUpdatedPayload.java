package com.dkds.cip.enrollment.common.event.payload;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogueUpdatedPayload(UUID itemId,
                                      String code,
                                      String description,
                                      BigDecimal reimbursementRate,
                                      boolean active) {
}
