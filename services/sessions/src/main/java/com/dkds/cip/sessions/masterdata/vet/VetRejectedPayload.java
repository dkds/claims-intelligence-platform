package com.dkds.cip.sessions.masterdata.vet;

import java.util.UUID;

public record VetRejectedPayload(UUID vetId,
                                 UUID clinicId,
                                 String rejectionReason) {
}
