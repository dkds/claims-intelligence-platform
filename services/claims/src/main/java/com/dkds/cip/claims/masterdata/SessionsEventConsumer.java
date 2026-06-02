package com.dkds.cip.claims.masterdata;

import com.dkds.cip.claims.claim.ClaimService;
import com.dkds.cip.claims.masterdata.session.SessionVerifiedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionsEventConsumer {

    private final JsonMapper jsonMapper;
    private final ClaimService claimService;

    @KafkaListener(topics = "${cip.kafka.topics.sessions}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            var envelope = jsonMapper.readTree(message);
            var eventType = envelope.get("eventType").asString();
            if ("session.verified".equals(eventType)) {
                var payload = jsonMapper.treeToValue(envelope.get("payload"), SessionVerifiedPayload.class);
                claimService.assembleFromSession(payload);
            } else {
                log.debug("Ignoring sessions event: {}", eventType);
            }
        } catch (Exception e) {
            // Phase 3: log and advance offset; Phase 7 will add DLT routing
            log.error("Failed to process sessions event: {}", e.getMessage(), e);
        }
    }
}
