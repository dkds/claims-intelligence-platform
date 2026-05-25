package com.dkds.cip.enrollment.vet;

import com.dkds.cip.enrollment.common.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class VetControllerTest extends AbstractIntegrationTest {

    private String clinicId;

    @BeforeEach
    void createClinic() {
        var requestBody = """
                { "name": "Vet Test Clinic" }
                """;
        clinicId = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void registerVet_returns201WithPendingStatus() {
        var requestBody = """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "email": "jane@vet.com",
                  "licenseNumber": "LIC-001"
                }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/vets", clinicId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("PENDING");
        assertThat(response.<String>path("clinicId")).isEqualTo(clinicId);
    }

    @Test
    void registerVet_withoutLicense_returns400() {
        var requestBody = """
                { "firstName": "Jane", "lastName": "Doe" }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/vets", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    void approveVet_transitionsToApproved() {
        var vetId = registerVet("LIC-APP-001");

        var response = given()
                .post("/vets/{id}/approve", vetId)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("APPROVED");
        assertThat(response.<String>path("updatedAt")).isNotNull();
    }

    @Test
    void approveAlreadyApprovedVet_returns409() {
        var vetId = registerVet("LIC-APP-002");
        given().post("/vets/{id}/approve", vetId).then().statusCode(200);

        var response = given()
                .post("/vets/{id}/approve", vetId)
                .then()
                .statusCode(409)
                .extract();

        assertThat(response.<String>path("error")).isEqualTo("Conflict");
    }

    @Test
    void rejectVet_setsRejectedWithReason() {
        var vetId = registerVet("LIC-REJ-001");
        var requestBody = """
                { "reason": "License expired" }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/vets/{id}/reject", vetId)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("REJECTED");
        assertThat(response.<String>path("rejectionReason")).isEqualTo("License expired");
    }

    @Test
    void rejectVet_withoutReason_returns400() {
        var vetId = registerVet("LIC-REJ-002");
        var requestBody = """
                { "reason": "" }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/vets/{id}/reject", vetId)
                .then()
                .statusCode(400);
    }

    @Test
    void rejectAlreadyRejectedVet_returns409() {
        var vetId = registerVet("LIC-REJ-003");
        var firstRejectBody = """
                { "reason": "Bad credentials" }
                """;
        given().contentType(ContentType.JSON)
                .body(firstRejectBody)
                .post("/vets/{id}/reject", vetId)
                .then().statusCode(200);

        var secondRejectBody = """
                { "reason": "Another reason" }
                """;
        given()
                .contentType(ContentType.JSON)
                .body(secondRejectBody)
                .post("/vets/{id}/reject", vetId)
                .then()
                .statusCode(409);
    }

    private String registerVet(String licenseNumber) {
        var requestBody = """
                { "firstName": "Jane", "lastName": "Doe", "licenseNumber": "%s" }
                """.formatted(licenseNumber);
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/vets", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}
