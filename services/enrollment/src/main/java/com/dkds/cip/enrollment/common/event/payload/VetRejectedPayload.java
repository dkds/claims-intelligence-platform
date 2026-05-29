package com.dkds.cip.enrollment.common.event.payload;

import java.util.UUID;

public record VetRejectedPayload(UUID vetId,
                                 UUID clinicId,
                                 String rejectionReason) {
}
