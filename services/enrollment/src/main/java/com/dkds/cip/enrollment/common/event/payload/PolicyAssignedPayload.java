package com.dkds.cip.enrollment.common.event.payload;

import java.time.LocalDate;
import java.util.UUID;

public record PolicyAssignedPayload(UUID policyId,
                                    UUID petId,
                                    String coverageType,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String status) {
}
