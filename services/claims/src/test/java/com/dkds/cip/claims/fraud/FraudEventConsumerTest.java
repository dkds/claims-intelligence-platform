package com.dkds.cip.claims.fraud;

import com.dkds.cip.claims.claim.Claim;
import com.dkds.cip.claims.claim.ClaimLine;
import com.dkds.cip.claims.claim.ClaimOrigin;
import com.dkds.cip.claims.claim.ClaimRepository;
import com.dkds.cip.claims.claim.ClaimStatus;
import com.dkds.cip.claims.common.AbstractIntegrationTest;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItemRepository;
import com.dkds.cip.claims.masterdata.clinic.LocalClinic;
import com.dkds.cip.claims.masterdata.clinic.LocalClinicRepository;
import com.dkds.cip.claims.masterdata.clinic.LocalClinicStatus;
import com.dkds.cip.claims.masterdata.pet.LocalPet;
import com.dkds.cip.claims.masterdata.pet.LocalPetRepository;
import com.dkds.cip.claims.masterdata.pet.LocalPetStatus;
import com.dkds.cip.claims.masterdata.policy.CoverageType;
import com.dkds.cip.claims.masterdata.policy.LocalPolicy;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyRepository;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FraudEventConsumerTest extends AbstractIntegrationTest {

    @Autowired
    FraudEventConsumer consumer;
    @Autowired
    ClaimRepository claimRepo;
    @Autowired
    LocalClinicRepository clinicRepo;
    @Autowired
    LocalPetRepository petRepo;
    @Autowired
    LocalPolicyRepository policyRepo;
    @Autowired
    LocalCatalogueItemRepository catalogueRepo;

    private UUID clinicId;
    private UUID petId;
    private UUID policyId;

    @BeforeEach
    void cleanup() {
        claimRepo.deleteAll();
        policyRepo.deleteAll();
        petRepo.deleteAll();
        clinicRepo.deleteAll();
        catalogueRepo.deleteAll();

        clinicId = UUID.randomUUID();
        petId = UUID.randomUUID();
        policyId = UUID.randomUUID();
        seedMasterData(CoverageType.STANDARD);
    }

    @Test
    void fraudScored_lowRisk_sessionOriginAssembled_autoAdjudicates() {
        var claimId = seedAssembledClaim(ClaimOrigin.SESSION);

        consumer.consume(fraudScoredEvent(claimId, "low"));

        var claim = claimRepo.findById(claimId).orElseThrow();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.READY_FOR_SUBMISSION);
        assertThat(claim.getAdjudicationDecision().name()).isEqualTo("PARTIALLY_APPROVED");
        // STANDARD = 80%; catalogue rate 100.00, qty 1 → approved = 80.00
        assertThat(claim.getApprovedAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    @Transactional
    void fraudScored_highRisk_sessionOriginAssembled_routesToReview() {
        var claimId = seedAssembledClaim(ClaimOrigin.SESSION);

        consumer.consume(fraudScoredEvent(claimId, "high"));

        var claim = claimRepo.findById(claimId).orElseThrow();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING_REVIEW);
        assertThat(claim.getAdjudicationDecision()).isNull();
        assertThat(claim.getTransitions())
                .anyMatch(t -> t.getFromStatus() == ClaimStatus.ASSEMBLED && t.getToStatus() == ClaimStatus.PENDING_REVIEW);
    }

    @Test
    void fraudScored_mediumRisk_sessionOriginAssembled_routesToReview() {
        var claimId = seedAssembledClaim(ClaimOrigin.SESSION);

        consumer.consume(fraudScoredEvent(claimId, "medium"));

        var claim = claimRepo.findById(claimId).orElseThrow();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING_REVIEW);
    }

    @Test
    void fraudScored_claimAlreadyOutOfAssembled_isNoOp() {
        var claimId = seedAssembledClaim(ClaimOrigin.SESSION);
        var claim = claimRepo.findById(claimId).orElseThrow();
        claim.setStatus(ClaimStatus.READY_FOR_SUBMISSION);
        claimRepo.save(claim);

        consumer.consume(fraudScoredEvent(claimId, "low"));

        var reloaded = claimRepo.findById(claimId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ClaimStatus.READY_FOR_SUBMISSION);
        assertThat(reloaded.getAdjudicationDecision()).isNull();
    }

    @Test
    void fraudScored_manualOriginClaim_isIgnoredRegardlessOfRisk() {
        var claimId = seedAssembledClaim(ClaimOrigin.MANUAL);

        consumer.consume(fraudScoredEvent(claimId, "low"));

        var claim = claimRepo.findById(claimId).orElseThrow();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.ASSEMBLED);
        assertThat(claim.getAdjudicationDecision()).isNull();
    }

    @Test
    void fraudScored_unknownClaimId_skipsSilently() {
        consumer.consume(fraudScoredEvent(UUID.randomUUID(), "low"));

        assertThat(claimRepo.count()).isZero();
    }

    private void seedMasterData(CoverageType coverageType) {
        var clinic = new LocalClinic();
        clinic.setId(clinicId);
        clinic.setName("Test Clinic");
        clinic.setStatus(LocalClinicStatus.ACTIVE);
        clinic.setUpdatedAt(Instant.now());
        clinicRepo.save(clinic);

        var pet = new LocalPet();
        pet.setId(petId);
        pet.setClinicId(clinicId);
        pet.setOwnerId(UUID.randomUUID());
        pet.setName("Rex");
        pet.setStatus(LocalPetStatus.ACTIVE);
        pet.setUpdatedAt(Instant.now());
        petRepo.save(pet);

        var policy = new LocalPolicy();
        policy.setId(policyId);
        policy.setPetId(petId);
        policy.setCoverageType(coverageType);
        policy.setStartDate(LocalDate.now().minusDays(30));
        policy.setEndDate(LocalDate.now().plusDays(335));
        policy.setStatus(LocalPolicyStatus.ACTIVE);
        policyRepo.save(policy);

        var item = new LocalCatalogueItem();
        item.setId(UUID.randomUUID());
        item.setCode("CONSULT");
        item.setDescription("General consultation");
        item.setReimbursementRate(new BigDecimal("100.00"));
        item.setActive(true);
        item.setUpdatedAt(Instant.now());
        catalogueRepo.save(item);
    }

    private UUID seedAssembledClaim(ClaimOrigin origin) {
        var claim = new Claim();
        claim.setClinicId(clinicId);
        claim.setPetId(petId);
        claim.setPolicyId(policyId);
        claim.setOrigin(origin);
        if (origin == ClaimOrigin.SESSION) {
            claim.setSourceSessionId(UUID.randomUUID());
        }
        claim.setSubmittedBy(UUID.randomUUID());
        claim.setStatus(ClaimStatus.ASSEMBLED);
        claim.setCreatedAt(Instant.now());

        var line = new ClaimLine();
        line.setClaim(claim);
        line.setProcedureCode("CONSULT");
        line.setQuantity(1);
        line.setRequestedAmount(new BigDecimal("100.00"));
        claim.getLines().add(line);
        claim.setTotalRequested(new BigDecimal("100.00"));

        return claimRepo.save(claim).getId();
    }

    private String fraudScoredEvent(UUID claimId, String riskLevel) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "claim.fraud-scored",
                  "eventVersion": 1,
                  "occurredAt": "2026-06-01T10:00:00Z",
                  "producer": "fraud-detection-service",
                  "tenantId": "%s",
                  "aggregateType": "claim",
                  "aggregateId": "%s",
                  "correlationId": "%s",
                  "causationId": null,
                  "payload": {
                    "claimId": "%s",
                    "score": 0.42,
                    "riskLevel": "%s",
                    "flags": [],
                    "modelVersion": "rules-2026.04",
                    "scoredAt": "2026-06-01T10:00:30Z"
                  }
                }
                """.formatted(
                UUID.randomUUID(), clinicId, claimId, UUID.randomUUID(),
                claimId, riskLevel
        );
    }
}
