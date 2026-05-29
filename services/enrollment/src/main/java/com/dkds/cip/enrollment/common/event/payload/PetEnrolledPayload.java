package com.dkds.cip.enrollment.common.event.payload;

import java.util.UUID;

public record PetEnrolledPayload(UUID petId,
                                 UUID clinicId,
                                 UUID ownerId,
                                 String name,
                                 String status) {
}
