package com.dkds.cip.claims.masterdata.policy;

import java.time.LocalDate;
import java.util.UUID;

public record PolicyAssignedPayload(UUID policyId, UUID petId, String coverageType,
                                    LocalDate startDate, LocalDate endDate, String status) {
}
