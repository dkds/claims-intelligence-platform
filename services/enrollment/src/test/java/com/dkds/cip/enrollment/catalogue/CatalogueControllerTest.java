package com.dkds.cip.enrollment.catalogue;

import com.dkds.cip.enrollment.common.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogueControllerTest extends AbstractIntegrationTest {

    @Test
    void createItem_returns201WithCodeUppercased() {
        var requestBody = """
                {
                  "code": "consult-01",
                  "description": "Standard consultation",
                  "reimbursementRate": 50.00
                }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/catalogue")
                .then()
                .statusCode(201)
                .extract();

        assertThat(response.<String>path("id")).isNotNull();
        assertThat(response.<String>path("code")).isEqualTo("CONSULT-01");
        assertThat(response.<Boolean>path("active")).isTrue();
    }

    @Test
    void createItem_withoutCode_returns400() {
        var requestBody = """
                {
                  "description": "Missing code",
                  "reimbursementRate": 10.00
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/catalogue")
                .then()
                .statusCode(400);
    }

    @Test
    void getItem_notFound_returns404() {
        var response = given()
                .get("/catalogue/{id}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .extract();

        assertThat(response.<String>path("error")).isEqualTo("Not Found");
    }

    @Test
    void getItem_byId_returns200() {
        var id = createItem("XRAY-01");

        var response = given()
                .get("/catalogue/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("id")).isEqualTo(id);
        assertThat(response.<String>path("code")).isEqualTo("XRAY-01");
    }

    @Test
    void listActive_includesCreatedItem() {
        var id = createItem("VACC-01");

        var ids = given()
                .get("/catalogue")
                .then()
                .statusCode(200)
                .extract().<List<String>>path("id");

        assertThat(ids).contains(id);
    }

    @Test
    void updateItem_returns200WithUpdatedDescription() {
        var id = createItem("BLOOD-01");
        var requestBody = """
                { "description": "Full blood panel" }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .put("/catalogue/{id}", id)
                .then()
                .statusCode(200)
                .extract();

        assertThat(response.<String>path("description")).isEqualTo("Full blood panel");
        assertThat(response.<String>path("code")).isEqualTo("BLOOD-01");
    }

    @Test
    void deactivateItem_returns204AndExcludesFromActiveList() {
        var id = createItem("TEMP-01");

        given()
                .delete("/catalogue/{id}", id)
                .then()
                .statusCode(204);

        var ids = given()
                .get("/catalogue")
                .then()
                .statusCode(200)
                .extract().<List<String>>path("id");

        assertThat(ids).doesNotContain(id);
    }

    private String createItem(String code) {
        var requestBody = """
                {
                  "code": "%s",
                  "description": "Test item",
                  "reimbursementRate": 25.00
                }
                """.formatted(code);
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/catalogue")
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}
