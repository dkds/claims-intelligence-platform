package com.dkds.cip.sessions.masterdata.catalogue;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogueUpdatedPayload(UUID itemId,
                                      String code,
                                      String description,
                                      BigDecimal reimbursementRate,
                                      boolean active) {
}
