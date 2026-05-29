package com.dkds.cip.sessions.common.event;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        UUID tenantId,
        String aggregateType,
        UUID aggregateId,
        UUID correlationId,
        UUID causationId,
        Object payload
) {
}
