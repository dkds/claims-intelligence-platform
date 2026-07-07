package com.dkds.cip.claims.fraud;

import com.dkds.cip.claims.claim.ClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventConsumer {

    private final JsonMapper jsonMapper;
    private final ClaimService claimService;

    @KafkaListener(topics = "${cip.kafka.topics.fraud}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            var envelope = jsonMapper.readTree(message);
            var eventType = envelope.get("eventType").asString();
            if ("claim.fraud-scored".equals(eventType)) {
                var payload = jsonMapper.treeToValue(envelope.get("payload"), FraudScoredPayload.class);
                claimService.handleFraudScored(payload);
            } else {
                log.debug("Ignoring fraud event: {}", eventType);
            }
        } catch (Exception e) {
            // Phase 3: log and advance offset; Phase 7 will add DLT routing
            log.error("Failed to process fraud event: {}", e.getMessage(), e);
        }
    }
}
