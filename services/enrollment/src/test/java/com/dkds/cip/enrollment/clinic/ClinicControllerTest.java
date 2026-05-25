package com.dkds.cip.enrollment.clinic;

import com.dkds.cip.enrollment.common.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class ClinicControllerTest extends AbstractIntegrationTest {

    @Test
    void registerClinic_returns201WithActiveStatus() {
        var requestBody = """
                {
                  "name": "Paws Clinic",
                  "addressLine1": "1 Main St",
                  "city": "London",
                  "postcode": "E1 1AA",
                  "countryCode": "GB",
                  "contactEmail": "info@paws.com"
                }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics")
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("id")).isNotNull();
        assertThat(response.<String>path("name")).isEqualTo("Paws Clinic");
        assertThat(response.<String>path("status")).isEqualTo("ACTIVE");
        assertThat(response.<String>path("registeredAt")).isNotNull();
    }

    @Test
    void registerClinic_withoutName_returns400() {
        var requestBody = """
                { "name": "" }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics")
                .then()
                .statusCode(400)
                .extract();

        assertThat(response.<String>path("error")).isEqualTo("Validation Failed");
    }

    @Test
    void getClinic_notFound_returns404() {
        var id = UUID.randomUUID();

        var response = given()
                .get("/clinics/{id}", id)
                .then()
                .statusCode(404)
                .extract();

        assertThat(response.<String>path("error")).isEqualTo("Not Found");
    }

    @Test
    void getClinic_byId_returns200() {
        var id = registerClinic("Green Park Vets");

        var response = given()
                .get("/clinics/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("id")).isEqualTo(id);
        assertThat(response.<String>path("name")).isEqualTo("Green Park Vets");
    }

    @Test
    void updateClinic_returns200WithUpdatedName() {
        var id = registerClinic("Old Name Clinic");
        var requestBody = """
                { "name": "New Name Clinic" }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .put("/clinics/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("name")).isEqualTo("New Name Clinic");
    }

    @Test
    void deactivateClinic_returns204AndSetsStatusSuspended() {
        var id = registerClinic("Closing Clinic");

        given()
                .delete("/clinics/{id}", id)
                .then()
                .statusCode(204);

        var response = given()
                .get("/clinics/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("status")).isEqualTo("SUSPENDED");
    }

    private String registerClinic(String name) {
        var requestBody = """
                { "name": "%s" }
                """.formatted(name);
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}
