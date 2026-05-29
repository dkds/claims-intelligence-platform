package com.dkds.cip.sessions.session.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionVerifiedPayload(
        UUID sessionId,
        UUID petId,
        UUID vetId,
        UUID clinicId,
        UUID verifiedBy,
        Instant verifiedAt,
        List<SessionLinePayload> lines
) {
}
