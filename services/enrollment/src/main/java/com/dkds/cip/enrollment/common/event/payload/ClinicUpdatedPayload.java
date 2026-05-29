package com.dkds.cip.enrollment.common.event.payload;

import java.util.UUID;

public record ClinicUpdatedPayload(UUID clinicId,
                                   String name,
                                   String status) {
}
