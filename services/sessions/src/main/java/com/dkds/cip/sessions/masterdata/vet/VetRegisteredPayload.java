package com.dkds.cip.sessions.masterdata.vet;

import java.util.UUID;

public record VetRegisteredPayload(UUID vetId,
                                   UUID clinicId,
                                   String firstName,
                                   String lastName,
                                   String status) {
}
