package com.dkds.cip.claims.claim;

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
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class ClaimControllerTest extends AbstractIntegrationTest {

    @Autowired LocalClinicRepository clinicRepo;
    @Autowired LocalPetRepository petRepo;
    @Autowired LocalPolicyRepository policyRepo;
    @Autowired LocalCatalogueItemRepository catalogueRepo;
    @Autowired ClaimRepository claimRepo;

    private UUID clinicId;
    private UUID petId;
    private UUID policyId;
    private UUID managerId;

    @BeforeEach
    void seedMasterData() {
        claimRepo.deleteAll();
        policyRepo.deleteAll();
        petRepo.deleteAll();
        clinicRepo.deleteAll();
        catalogueRepo.deleteAll();

        clinicId = UUID.randomUUID();
        petId = UUID.randomUUID();
        policyId = UUID.randomUUID();
        managerId = UUID.randomUUID();

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
        policy.setCoverageType(CoverageType.STANDARD);
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

    @Test
    void submitManualClaim_validData_returns201AndAdjudicates() {
        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "requestedAmount": 100.00 }]
                }
                """.formatted(petId, policyId, managerId);

        var response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("id")).isNotNull();
        assertThat(response.<String>path("origin")).isEqualTo("MANUAL");
        // standard policy = 80% coverage → partially approved at 80.00
        assertThat(response.<String>path("status")).isEqualTo("READY_FOR_SUBMISSION");
        assertThat(response.<String>path("adjudicationDecision")).isEqualTo("PARTIALLY_APPROVED");
        assertThat(response.<Float>path("approvedAmount")).isEqualTo(80.00f);
        assertThat(response.<Integer>path("lines.size()")).isEqualTo(1);
    }

    @Test
    void submitManualClaim_premiumPolicy_fullyApproves() {
        policyRepo.findById(policyId).ifPresent(p -> {
            p.setCoverageType(CoverageType.PREMIUM);
            policyRepo.save(p);
        });

        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "requestedAmount": 100.00 }]
                }
                """.formatted(petId, policyId, managerId);

        var response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("READY_FOR_SUBMISSION");
        assertThat(response.<String>path("adjudicationDecision")).isEqualTo("APPROVED");
        assertThat(response.<Float>path("approvedAmount")).isEqualTo(100.00f);
    }

    @Test
    void submitManualClaim_uncoveredProcedure_claimRejected() {
        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "UNKNOWN-PROC", "quantity": 1, "requestedAmount": 50.00 }]
                }
                """.formatted(petId, policyId, managerId);

        var response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("REJECTED");
        assertThat(response.<String>path("adjudicationDecision")).isNull();
    }

    @Test
    void submitManualClaim_petNotFound_returns404() {
        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "requestedAmount": 100.00 }]
                }
                """.formatted(UUID.randomUUID(), policyId, managerId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(404);
    }

    @Test
    void submitManualClaim_inactivePet_returns409() {
        petRepo.findById(petId).ifPresent(p -> {
            p.setStatus(LocalPetStatus.INACTIVE);
            petRepo.save(p);
        });

        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "requestedAmount": 100.00 }]
                }
                """.formatted(petId, policyId, managerId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void submitManualClaim_policyNotActive_returns409() {
        policyRepo.findById(policyId).ifPresent(p -> {
            p.setStatus(LocalPolicyStatus.EXPIRED);
            policyRepo.save(p);
        });

        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "requestedAmount": 100.00 }]
                }
                """.formatted(petId, policyId, managerId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void submitManualClaim_policyWrongPet_returns409() {
        var otherPet = new LocalPet();
        otherPet.setId(UUID.randomUUID());
        otherPet.setClinicId(clinicId);
        otherPet.setOwnerId(UUID.randomUUID());
        otherPet.setName("OtherPet");
        otherPet.setStatus(LocalPetStatus.ACTIVE);
        petRepo.save(otherPet);

        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "requestedAmount": 100.00 }]
                }
                """.formatted(otherPet.getId(), policyId, managerId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void submitManualClaim_missingFields_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    void getClaim_validId_returns200() {
        var claimId = submitClaim();

        var response = given()
                .get("/claims/{claimId}", claimId)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("id")).isEqualTo(claimId);
    }

    @Test
    void getClaim_notFound_returns404() {
        given()
                .get("/claims/{claimId}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void listByClinic_returnsClaims() {
        submitClaim();
        submitClaim();

        var response = given()
                .get("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(200)
                .extract()
                .<java.util.List<?>>path("$");

        assertThat(response).hasSize(2);
    }

    @Test
    void listByClinic_filteredByStatus_returnsMatchingClaims() {
        submitClaim();

        var response = given()
                .queryParam("status", "READY_FOR_SUBMISSION")
                .get("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(200)
                .extract()
                .<java.util.List<?>>path("$");

        assertThat(response).hasSize(1);
    }

    private String submitClaim() {
        var body = """
                {
                  "petId": "%s",
                  "policyId": "%s",
                  "submittedBy": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "requestedAmount": 100.00 }]
                }
                """.formatted(petId, policyId, managerId);

        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/claims", clinicId)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
