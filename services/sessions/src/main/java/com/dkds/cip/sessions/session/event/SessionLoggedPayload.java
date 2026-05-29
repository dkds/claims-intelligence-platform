package com.dkds.cip.sessions.session.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionLoggedPayload(
        UUID sessionId,
        UUID petId,
        UUID vetId,
        UUID clinicId,
        Instant loggedAt,
        List<SessionLinePayload> lines
) {
}
