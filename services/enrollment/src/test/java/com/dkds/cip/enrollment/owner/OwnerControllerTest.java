package com.dkds.cip.enrollment.owner;

import com.dkds.cip.enrollment.common.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class OwnerControllerTest extends AbstractIntegrationTest {

    private String clinicId;

    @BeforeEach
    void createClinic() {
        var requestBody = """
                { "name": "Owner Test Clinic" }
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
    void registerOwner_returns201WithClinicId() {
        var requestBody = """
                {
                  "firstName": "Alice",
                  "lastName": "Smith",
                  "email": "alice@example.com"
                }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/owners", clinicId)
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("id")).isNotNull();
        assertThat(response.<String>path("firstName")).isEqualTo("Alice");
        assertThat(response.<String>path("lastName")).isEqualTo("Smith");
        assertThat(response.<String>path("clinicId")).isEqualTo(clinicId);
    }

    @Test
    void registerOwner_withoutFirstName_returns400() {
        var requestBody = """
                { "lastName": "Smith" }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/owners", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    void getOwner_notFound_returns404() {
        var response = given()
                .get("/owners/{id}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .extract();

        assertThat(response.<String>path("error")).isEqualTo("Not Found");
    }

    @Test
    void getOwner_byId_returns200() {
        var id = registerOwner("Bob", "Jones");

        var response = given()
                .get("/owners/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("id")).isEqualTo(id);
        assertThat(response.<String>path("firstName")).isEqualTo("Bob");
    }

    @Test
    void listOwnersByClinic_includesRegisteredOwner() {
        var id = registerOwner("Carol", "White");

        var ids = given()
                .get("/clinics/{id}/owners", clinicId)
                .then()
                .statusCode(200)
                .extract().<List<String>>path("id");

        assertThat(ids).contains(id);
    }

    @Test
    void updateOwner_returns200WithUpdatedEmail() {
        var id = registerOwner("Dave", "Brown");
        var requestBody = """
                { "email": "dave.updated@example.com" }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .put("/owners/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("email")).isEqualTo("dave.updated@example.com");
        assertThat(response.<String>path("firstName")).isEqualTo("Dave");
    }

    private String registerOwner(String firstName, String lastName) {
        var requestBody = """
                { "firstName": "%s", "lastName": "%s" }
                """.formatted(firstName, lastName);
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics/{id}/owners", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}
