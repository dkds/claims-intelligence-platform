package com.dkds.cip.enrollment.common.event.payload;

import java.util.UUID;

public record ClinicRegisteredPayload(UUID clinicId,
                                      String name,
                                      String status) {
}
