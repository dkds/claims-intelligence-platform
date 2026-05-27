package com.dkds.cip.enrollment.policy;

import com.dkds.cip.enrollment.common.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyControllerTest extends AbstractIntegrationTest {

    private String petId;

    @BeforeEach
    void createClinicOwnerAndPet() {
        var clinicBody = """
                { "name": "Policy Test Clinic" }
                """;
        var clinicId = given()
                .contentType(ContentType.JSON)
                .body(clinicBody)
                .post("/clinics")
                .then()
                .statusCode(201)
                .extract().<String>path("id");

        var ownerBody = """
                { "firstName": "Frank", "lastName": "Green" }
                """;
        var ownerId = given()
                .contentType(ContentType.JSON)
                .body(ownerBody)
                .post("/clinics/{id}/owners", clinicId)
                .then()
                .statusCode(201)
                .extract().<String>path("id");

        var petBody = """
                { "ownerId": "%s", "name": "Rex", "species": "Dog" }
                """.formatted(ownerId);
        petId = given()
                .contentType(ContentType.JSON)
                .body(petBody)
                .post("/clinics/{id}/pets", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void assignPolicy_returns201WithActiveStatus() {
        var requestBody = """
                {
                  "coverageType": "STANDARD",
                  "startDate": "2026-01-01",
                  "endDate": "2026-12-31"
                }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/pets/{id}/policies", petId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("id")).isNotNull();
        assertThat(response.<String>path("coverageType")).isEqualTo("STANDARD");
        assertThat(response.<String>path("status")).isEqualTo("ACTIVE");
        assertThat(response.<String>path("petId")).isEqualTo(petId);
    }

    @Test
    void assignPolicy_withoutCoverageType_returns400() {
        var requestBody = """
                {
                  "startDate": "2026-01-01",
                  "endDate": "2026-12-31"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/pets/{id}/policies", petId)
                .then()
                .statusCode(400);
    }

    @Test
    void getPolicy_notFound_returns404() {
        var response = given()
                .get("/policies/{id}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .extract();

        assertThat(response.<String>path("error")).isEqualTo("Not Found");
    }

    @Test
    void getPolicy_byId_returns200() {
        var id = assignPolicy("BASIC");

        var response = given()
                .get("/policies/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("id")).isEqualTo(id);
        assertThat(response.<String>path("coverageType")).isEqualTo("BASIC");
    }

    @Test
    void listPoliciesByPet_includesAssignedPolicy() {
        var id = assignPolicy("PREMIUM");

        var ids = given()
                .get("/pets/{id}/policies", petId)
                .then()
                .statusCode(200)
                .extract().<List<String>>path("id");

        assertThat(ids).contains(id);
    }

    @Test
    void updatePolicy_returns200WithUpdatedEndDate() {
        var id = assignPolicy("STANDARD");
        var requestBody = """
                { "endDate": "2027-06-30" }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .put("/policies/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("endDate")).isEqualTo("2027-06-30");
    }

    private String assignPolicy(String coverageType) {
        var requestBody = """
                {
                  "coverageType": "%s",
                  "startDate": "2026-01-01",
                  "endDate": "2026-12-31"
                }
                """.formatted(coverageType);
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/pets/{id}/policies", petId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}
