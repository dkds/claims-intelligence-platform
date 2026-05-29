package com.dkds.cip.enrollment.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${cip.kafka.topics.enrollment}")
    private String topic;

    public void publish(String eventType, String aggregateType, UUID aggregateId,
                        UUID tenantId, Object payload) {
        try {
            var event = new DomainEvent(
                    UUID.randomUUID(),
                    eventType,
                    1,
                    Instant.now(),
                    "enrollment-service",
                    tenantId,
                    aggregateType,
                    aggregateId,
                    UUID.randomUUID(),
                    null,
                    payload
            );
            var json = jsonMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, aggregateId.toString(), json);
            log.info("Published {} for {} {}", eventType, aggregateType, aggregateId);
        } catch (Exception e) {
            // Phase 2: swallow — Phase 7 will replace with transactional outbox
            log.error("Failed to publish {} for {} {}: {}", eventType, aggregateType, aggregateId,
                    e.getMessage(), e);
        }
    }
}
