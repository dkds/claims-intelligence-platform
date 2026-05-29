package com.dkds.cip.sessions.masterdata.vet;

import java.util.UUID;

public record VetApprovedPayload(UUID vetId, UUID clinicId) {
}
