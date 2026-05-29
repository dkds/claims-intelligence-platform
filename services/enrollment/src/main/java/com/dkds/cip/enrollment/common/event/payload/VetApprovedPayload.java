package com.dkds.cip.enrollment.common.event.payload;

import java.util.UUID;

public record VetApprovedPayload(UUID vetId, UUID clinicId) {
}
