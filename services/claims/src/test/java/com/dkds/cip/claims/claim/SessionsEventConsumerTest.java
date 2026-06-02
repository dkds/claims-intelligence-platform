package com.dkds.cip.claims.claim;

import com.dkds.cip.claims.common.AbstractIntegrationTest;
import com.dkds.cip.claims.masterdata.SessionsEventConsumer;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SessionsEventConsumerTest extends AbstractIntegrationTest {

    @Autowired
    SessionsEventConsumer consumer;
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

    @BeforeEach
    void cleanup() {
        claimRepo.deleteAll();
        policyRepo.deleteAll();
        petRepo.deleteAll();
        clinicRepo.deleteAll();
        catalogueRepo.deleteAll();
    }

    @Test
    void sessionVerified_knownPetStandardPolicy_assemblesAndAdjudicatesClaim() {
        clinicId = UUID.randomUUID();
        petId = UUID.randomUUID();
        seedMasterData(CoverageType.STANDARD);

        consumer.consume(sessionVerifiedEvent(clinicId, petId, "CONSULT", 1));

        var claims = claimRepo.findAll();
        assertThat(claims).hasSize(1);

        var claim = claims.get(0);
        assertThat(claim.getOrigin()).isEqualTo(ClaimOrigin.SESSION);
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.READY_FOR_SUBMISSION);
        assertThat(claim.getAdjudicationDecision().name()).isEqualTo("PARTIALLY_APPROVED");
        // STANDARD = 80%; catalogue rate 100.00, qty 1 → approved = 80.00
        assertThat(claim.getApprovedAmount()).isEqualByComparingTo("80.00");
        assertThat(claim.getSourceSessionId()).isNotNull();
    }

    @Test
    void sessionVerified_premiumPolicy_fullyApprovesClaim() {
        clinicId = UUID.randomUUID();
        petId = UUID.randomUUID();
        seedMasterData(CoverageType.PREMIUM);

        consumer.consume(sessionVerifiedEvent(clinicId, petId, "CONSULT", 1));

        var claim = claimRepo.findAll().get(0);
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.READY_FOR_SUBMISSION);
        assertThat(claim.getAdjudicationDecision().name()).isEqualTo("APPROVED");
        assertThat(claim.getApprovedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void sessionVerified_unknownPet_skipsSilently() {
        consumer.consume(sessionVerifiedEvent(UUID.randomUUID(), UUID.randomUUID(), "CONSULT", 1));

        assertThat(claimRepo.count()).isZero();
    }

    @Test
    void sessionVerified_petHasNoActivePolicy_skipsSilently() {
        clinicId = UUID.randomUUID();
        petId = UUID.randomUUID();

        var clinic = new LocalClinic();
        clinic.setId(clinicId);
        clinic.setName("Test Clinic");
        clinic.setStatus(LocalClinicStatus.ACTIVE);
        clinicRepo.save(clinic);

        var pet = new LocalPet();
        pet.setId(petId);
        pet.setClinicId(clinicId);
        pet.setOwnerId(UUID.randomUUID());
        pet.setName("Rex");
        pet.setStatus(LocalPetStatus.ACTIVE);
        petRepo.save(pet);

        // No policy seeded

        consumer.consume(sessionVerifiedEvent(clinicId, petId, "CONSULT", 1));

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
        policy.setId(UUID.randomUUID());
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

    private String sessionVerifiedEvent(UUID clinicId, UUID petId, String procedureCode, int quantity) {
        var sessionId = UUID.randomUUID();
        var vetId = UUID.randomUUID();
        var verifiedBy = UUID.randomUUID();
        return """
                {
                  "eventId": "%s",
                  "eventType": "session.verified",
                  "eventVersion": 1,
                  "occurredAt": "2026-06-01T10:00:00Z",
                  "producer": "sessions-service",
                  "tenantId": "%s",
                  "aggregateType": "session",
                  "aggregateId": "%s",
                  "correlationId": "%s",
                  "causationId": null,
                  "payload": {
                    "sessionId": "%s",
                    "petId": "%s",
                    "vetId": "%s",
                    "clinicId": "%s",
                    "verifiedBy": "%s",
                    "verifiedAt": "2026-06-01T10:00:00Z",
                    "lines": [{ "procedureCode": "%s", "quantity": %d, "notes": null }]
                  }
                }
                """.formatted(
                UUID.randomUUID(), clinicId, sessionId, UUID.randomUUID(),
                sessionId, petId, vetId, clinicId, verifiedBy,
                procedureCode, quantity
        );
    }
}
