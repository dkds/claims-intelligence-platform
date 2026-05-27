package com.dkds.cip.enrollment.pet;

import com.dkds.cip.enrollment.common.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class PetControllerTest extends AbstractIntegrationTest {

    private String clinicId;
    private String ownerId;

    @BeforeEach
    void createClinicAndOwner() {
        var clinicBody = """
                { "name": "Pet Test Clinic" }
                """;
        clinicId = given()
                .contentType(ContentType.JSON)
                .body(clinicBody)
                .post("/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");

        var ownerBody = """
                { "firstName": "Eve", "lastName": "Taylor" }
                """;
        ownerId = given()
                .contentType(ContentType.JSON)
                .body(ownerBody)
                .post("/clinics/{id}/owners", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void enrolPet_returns201WithActiveStatus() {
        var requestBody = """
                {
                  "ownerId": "%s",
                  "name": "Buddy",
                  "species": "Dog",
                  "breed": "Labrador",
                  "dateOfBirth": "2020-03-15"
                }
                """.formatted(ownerId);

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/pets", clinicId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("id")).isNotNull();
        assertThat(response.<String>path("name")).isEqualTo("Buddy");
        assertThat(response.<String>path("status")).isEqualTo("ACTIVE");
        assertThat(response.<String>path("clinicId")).isEqualTo(clinicId);
        assertThat(response.<String>path("ownerId")).isEqualTo(ownerId);
    }

    @Test
    void enrolPet_withoutName_returns400() {
        var requestBody = """
                { "ownerId": "%s", "species": "Cat" }
                """.formatted(ownerId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/pets", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    void getPet_notFound_returns404() {
        var response = given()
                .get("/pets/{id}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .extract();

        assertThat(response.<String>path("error")).isEqualTo("Not Found");
    }

    @Test
    void getPet_byId_returns200() {
        var id = enrolPet("Whiskers", "Cat");

        var response = given()
                .get("/pets/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("id")).isEqualTo(id);
        assertThat(response.<String>path("name")).isEqualTo("Whiskers");
    }

    @Test
    void listPetsByClinic_includesEnrolledPet() {
        var id = enrolPet("Max", "Dog");

        var ids = given()
                .get("/clinics/{id}/pets", clinicId)
                .then()
                .statusCode(200)
                .extract().<List<String>>path("id");

        assertThat(ids).contains(id);
    }

    @Test
    void updatePet_returns200WithUpdatedName() {
        var id = enrolPet("OldName", "Rabbit");
        var requestBody = """
                { "name": "NewName" }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .put("/pets/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("name")).isEqualTo("NewName");
        assertThat(response.<String>path("species")).isEqualTo("Rabbit");
    }

    private String enrolPet(String name, String species) {
        var requestBody = """
                { "ownerId": "%s", "name": "%s", "species": "%s" }
                """.formatted(ownerId, name, species);
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/pets", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}
