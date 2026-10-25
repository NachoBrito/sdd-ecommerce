package es.nachobrito.sdd.ecommerce.infrastructure;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying health and prometheus metrics endpoints.
 */
@MicronautTest(environments = "test")
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class HealthCheckIntegrationTest {

    @Test
    @DisplayName("GET /health returns 200 with UP status")
    void healthEndpointReturnsUp(final RequestSpecification spec) {
        given(spec)
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("GET /prometheus returns 200 with prometheus metrics")
    void prometheusEndpointReturnsMetrics(final RequestSpecification spec) {
        given(spec)
                .when()
                .get("/prometheus")
                .then()
                .statusCode(200)
                .body(containsString("jvm_"));
    }
}
