package com.dkds.cip.claims.masterdata.clinic;

import java.util.UUID;

public record ClinicUpdatedPayload(UUID clinicId, String name, String status) {
}
