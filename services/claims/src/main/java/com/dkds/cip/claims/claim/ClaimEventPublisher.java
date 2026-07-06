package com.dkds.cip.claims.claim;

import com.dkds.cip.claims.claim.event.*;
import com.dkds.cip.claims.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cip.kafka.topics.claims}")
    private String topic;

    public void publishAssembled(Claim claim) {
        var payload = new ClaimAssembledPayload(
                claim.getId(),
                claim.getClinicId(),
                claim.getPetId(),
                claim.getPolicyId(),
                claim.getOrigin().name().toLowerCase(),
                claim.getSourceSessionId(),
                claim.getSubmittedBy(),
                claim.getLines().stream()
                        .map(l -> new ClaimLinePayload(l.getProcedureCode(), l.getQuantity(), l.getRequestedAmount()))
                        .toList(),
                claim.getTotalRequested(),
                claim.getCreatedAt()
        );
        publish("claim.assembled", claim.getId(), claim.getClinicId(), payload);
    }

    public void publishAdjudicated(Claim claim, List<String> reasons) {
        var decision = claim.getAdjudicationDecision().name().toLowerCase().replace("_", "-");
        var payload = new ClaimAdjudicatedPayload(
                claim.getId(),
                decision,
                claim.getApprovedAmount(),
                reasons,
                "auto",
                claim.getOrigin().name().toLowerCase(),
                claim.getSourceSessionId(),
                claim.getUpdatedAt()
        );
        publish("claim.adjudicated", claim.getId(), claim.getClinicId(), payload);
    }

    public void publishRejected(Claim claim, java.util.List<String> reasons) {
        var payload = new ClaimRejectedPayload(
                claim.getId(),
                reasons,
                "auto",
                claim.getOrigin().name().toLowerCase(),
                claim.getUpdatedAt()
        );
        publish("claim.rejected", claim.getId(), claim.getClinicId(), payload);
    }

    public void publishReadyForSubmission(Claim claim) {
        var decision = claim.getAdjudicationDecision().name().toLowerCase().replace("_", "-");
        var payload = new ClaimReadyForSubmissionPayload(
                claim.getId(),
                claim.getClinicId(),
                claim.getPetId(),
                claim.getPolicyId(),
                decision,
                claim.getApprovedAmount(),
                claim.getOrigin().name().toLowerCase(),
                claim.getSourceSessionId(),
                claim.getUpdatedAt()
        );
        publish("claim.ready-for-submission", claim.getId(), claim.getClinicId(), payload);
    }

    private void publish(String eventType, UUID aggregateId, UUID tenantId, Object payload) {
        try {
            var event = new DomainEvent(
                    UUID.randomUUID(),
                    eventType,
                    1,
                    Instant.now(),
                    "claims-service",
                    tenantId,
                    "claim",
                    aggregateId,
                    UUID.randomUUID(),
                    null,
                    payload
            );
            var json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, aggregateId.toString(), json);
            log.info("Published {} for claim {}", eventType, aggregateId);
        } catch (Exception e) {
            // Phase 3: swallow — Phase 7 will replace with transactional outbox
            log.error("Failed to publish {} for claim {}: {}", eventType, aggregateId, e.getMessage(), e);
        }
    }
}
