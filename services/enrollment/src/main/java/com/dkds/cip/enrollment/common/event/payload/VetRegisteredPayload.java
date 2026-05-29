package com.dkds.cip.enrollment.common.event.payload;

import java.util.UUID;

public record VetRegisteredPayload(UUID vetId,
                                   UUID clinicId,
                                   String firstName,
                                   String lastName,
                                   String status) {
}
