package com.dkds.cip.sessions.masterdata.pet;

import java.util.UUID;

public record PetEnrolledPayload(UUID petId,
                                 UUID clinicId,
                                 UUID ownerId,
                                 String name,
                                 String status) {
}
