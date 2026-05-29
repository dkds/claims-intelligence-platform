package com.dkds.cip.sessions.session;

import com.dkds.cip.sessions.common.event.DomainEvent;
import com.dkds.cip.sessions.session.event.SessionLinePayload;
import com.dkds.cip.sessions.session.event.SessionLoggedPayload;
import com.dkds.cip.sessions.session.event.SessionVerifiedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cip.kafka.topics.sessions}")
    private String topic;

    public void publishSessionLogged(Session session) {
        var payload = new SessionLoggedPayload(
                session.getId(), session.getPetId(), session.getVetId(), session.getClinicId(),
                session.getLoggedAt(),
                session.getLines().stream()
                        .map(l -> new SessionLinePayload(l.getProcedureCode(), l.getQuantity(), l.getNotes()))
                        .toList()
        );
        publish("session.logged", session.getId(), session.getClinicId(), payload);
    }

    public void publishSessionVerified(Session session) {
        var payload = new SessionVerifiedPayload(
                session.getId(), session.getPetId(), session.getVetId(), session.getClinicId(),
                session.getVerifiedBy(), session.getVerifiedAt(),
                session.getLines().stream()
                        .map(l -> new SessionLinePayload(l.getProcedureCode(), l.getQuantity(), l.getNotes()))
                        .toList()
        );
        publish("session.verified", session.getId(), session.getClinicId(), payload);
    }

    private void publish(String eventType, UUID aggregateId, UUID tenantId, Object payload) {
        try {
            var event = new DomainEvent(
                    UUID.randomUUID(),
                    eventType,
                    1,
                    Instant.now(),
                    "sessions-service",
                    tenantId,
                    "session",
                    aggregateId,
                    UUID.randomUUID(),
                    null,
                    payload
            );
            var json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, aggregateId.toString(), json);
            log.info("Published {} for session {}", eventType, aggregateId);
        } catch (Exception e) {
            // Phase 2: swallow — Phase 7 will replace with transactional outbox
            log.error("Failed to publish {} for session {}: {}", eventType, aggregateId,
                    e.getMessage(), e);
        }
    }
}
