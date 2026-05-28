package com.dkds.cip.sessions.session;

import com.dkds.cip.sessions.common.AbstractIntegrationTest;
import com.dkds.cip.sessions.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.sessions.masterdata.catalogue.LocalCatalogueItemRepository;
import com.dkds.cip.sessions.masterdata.clinic.LocalClinic;
import com.dkds.cip.sessions.masterdata.clinic.LocalClinicRepository;
import com.dkds.cip.sessions.masterdata.clinic.LocalClinicStatus;
import com.dkds.cip.sessions.masterdata.pet.LocalPet;
import com.dkds.cip.sessions.masterdata.pet.LocalPetRepository;
import com.dkds.cip.sessions.masterdata.pet.LocalPetStatus;
import com.dkds.cip.sessions.masterdata.policy.CoverageType;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicy;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicyRepository;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicyStatus;
import com.dkds.cip.sessions.masterdata.vet.LocalVet;
import com.dkds.cip.sessions.masterdata.vet.LocalVetRepository;
import com.dkds.cip.sessions.masterdata.vet.LocalVetStatus;
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

class SessionControllerTest extends AbstractIntegrationTest {

    @Autowired
    LocalClinicRepository clinicRepo;
    @Autowired
    LocalVetRepository vetRepo;
    @Autowired
    LocalPetRepository petRepo;
    @Autowired
    LocalPolicyRepository policyRepo;
    @Autowired
    LocalCatalogueItemRepository catalogueRepo;
    @Autowired
    SessionRepository sessionRepo;

    private UUID clinicId;
    private UUID vetId;
    private UUID petId;

    @BeforeEach
    void seedMasterData() {
        sessionRepo.deleteAll();
        policyRepo.deleteAll();
        petRepo.deleteAll();
        vetRepo.deleteAll();
        clinicRepo.deleteAll();
        catalogueRepo.deleteAll();

        clinicId = UUID.randomUUID();
        vetId = UUID.randomUUID();
        petId = UUID.randomUUID();

        var clinic = new LocalClinic();
        clinic.setId(clinicId);
        clinic.setName("Test Clinic");
        clinic.setStatus(LocalClinicStatus.ACTIVE);
        clinic.setUpdatedAt(Instant.now());
        clinicRepo.save(clinic);

        var vet = new LocalVet();
        vet.setId(vetId);
        vet.setClinicId(clinicId);
        vet.setFirstName("Jane");
        vet.setLastName("Doe");
        vet.setStatus(LocalVetStatus.APPROVED);
        vet.setUpdatedAt(Instant.now());
        vetRepo.save(vet);

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
        policy.setCoverageType(CoverageType.STANDARD);
        policy.setStartDate(LocalDate.now().minusDays(30));
        policy.setEndDate(LocalDate.now().plusDays(335));
        policy.setStatus(LocalPolicyStatus.ACTIVE);
        policyRepo.save(policy);

        var item = new LocalCatalogueItem();
        item.setId(UUID.randomUUID());
        item.setCode("CONSULT");
        item.setDescription("General consultation");
        item.setReimbursementRate(new BigDecimal("80.00"));
        item.setActive(true);
        item.setUpdatedAt(Instant.now());
        catalogueRepo.save(item);
    }

    @Test
    void logSession_validData_returns201() {
        var body = """
                {
                  "vetId": "%s",
                  "petId": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1, "notes": "Annual checkup" }]
                }
                """.formatted(vetId, petId);

        var response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("id")).isNotNull();
        assertThat(response.<String>path("status")).isEqualTo("LOGGED");
        assertThat(response.<String>path("loggedAt")).isNotNull();
        assertThat(response.<Integer>path("lines.size()")).isEqualTo(1);
        assertThat(response.<String>path("lines[0].procedureCode")).isEqualTo("CONSULT");
    }

    @Test
    void logSession_pendingVet_returns409() {
        var pendingVetId = UUID.randomUUID();
        var pendingVet = new LocalVet();
        pendingVet.setId(pendingVetId);
        pendingVet.setClinicId(clinicId);
        pendingVet.setFirstName("Pending");
        pendingVet.setLastName("Vet");
        pendingVet.setStatus(LocalVetStatus.PENDING);
        pendingVet.setUpdatedAt(Instant.now());
        vetRepo.save(pendingVet);

        var body = """
                {
                  "vetId": "%s",
                  "petId": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1 }]
                }
                """.formatted(pendingVetId, petId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void logSession_vetWrongClinic_returns409() {
        var otherClinicId = UUID.randomUUID();
        var otherClinic = new LocalClinic();
        otherClinic.setId(otherClinicId);
        otherClinic.setName("Other Clinic");
        otherClinic.setStatus(LocalClinicStatus.ACTIVE);
        clinicRepo.save(otherClinic);

        var body = """
                {
                  "vetId": "%s",
                  "petId": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1 }]
                }
                """.formatted(vetId, petId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", otherClinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void logSession_inactivePet_returns409() {
        var inactivePetId = UUID.randomUUID();
        var inactivePet = new LocalPet();
        inactivePet.setId(inactivePetId);
        inactivePet.setClinicId(clinicId);
        inactivePet.setOwnerId(UUID.randomUUID());
        inactivePet.setName("InactivePet");
        inactivePet.setStatus(LocalPetStatus.INACTIVE);
        petRepo.save(inactivePet);

        var body = """
                {
                  "vetId": "%s",
                  "petId": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1 }]
                }
                """.formatted(vetId, inactivePetId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void logSession_noActivePolicy_returns409() {
        var noPolicyPetId = UUID.randomUUID();
        var noPolicyPet = new LocalPet();
        noPolicyPet.setId(noPolicyPetId);
        noPolicyPet.setClinicId(clinicId);
        noPolicyPet.setOwnerId(UUID.randomUUID());
        noPolicyPet.setName("NoPolicyPet");
        noPolicyPet.setStatus(LocalPetStatus.ACTIVE);
        petRepo.save(noPolicyPet);

        var body = """
                {
                  "vetId": "%s",
                  "petId": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1 }]
                }
                """.formatted(vetId, noPolicyPetId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void logSession_unknownProcedureCode_returns409() {
        var body = """
                {
                  "vetId": "%s",
                  "petId": "%s",
                  "lines": [{ "procedureCode": "UNKNOWN-CODE", "quantity": 1 }]
                }
                """.formatted(vetId, petId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    void logSession_missingFields_returns400() {
        var body = """
                {
                  "vetId": "%s"
                }
                """.formatted(vetId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    void verifySession_loggedSession_returns200WithVerifiedStatus() {
        var sessionId = logSession();
        var managerId = UUID.randomUUID();

        var response = given()
                .contentType(ContentType.JSON)
                .body("""
                        { "verifiedBy": "%s" }
                        """.formatted(managerId))
                .post("/sessions/{id}/verify", sessionId)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("VERIFIED");
        assertThat(response.<String>path("verifiedAt")).isNotNull();
        assertThat(response.<String>path("verifiedBy")).isEqualTo(managerId.toString());
    }

    @Test
    void verifySession_alreadyVerified_returns409() {
        var sessionId = logSession();
        var managerId = UUID.randomUUID();
        var body = """
                { "verifiedBy": "%s" }
                """.formatted(managerId);

        given().contentType(ContentType.JSON).body(body)
                .post("/sessions/{id}/verify", sessionId).then().statusCode(200);

        given().contentType(ContentType.JSON).body(body)
                .post("/sessions/{id}/verify", sessionId).then().statusCode(409);
    }

    @Test
    void cancelSession_loggedSession_returns200WithCancelledStatus() {
        var sessionId = logSession();

        var response = given()
                .post("/sessions/{id}/cancel", sessionId)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("CANCELLED");
    }

    @Test
    void cancelSession_verifiedSession_returns409() {
        var sessionId = logSession();
        var managerId = UUID.randomUUID();

        given().contentType(ContentType.JSON)
                .body("""
                        { "verifiedBy": "%s" }
                        """.formatted(managerId))
                .post("/sessions/{id}/verify", sessionId).then().statusCode(200);

        given()
                .post("/sessions/{id}/cancel", sessionId)
                .then()
                .statusCode(409);
    }

    @Test
    void getSession_notFound_returns404() {
        given()
                .get("/sessions/{id}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void listByClinic_returnsSessions() {
        logSession();
        logSession();

        var response = given()
                .get("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(200)
                .extract()
                .<java.util.List<?>>path("$");

        assertThat(response).hasSize(2);
    }

    private String logSession() {
        var body = """
                {
                  "vetId": "%s",
                  "petId": "%s",
                  "lines": [{ "procedureCode": "CONSULT", "quantity": 1 }]
                }
                """.formatted(vetId, petId);

        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/clinics/{clinicId}/sessions", clinicId)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
