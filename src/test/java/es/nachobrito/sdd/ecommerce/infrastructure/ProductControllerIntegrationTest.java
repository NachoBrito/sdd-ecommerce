package es.nachobrito.sdd.ecommerce.infrastructure;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Integration test verifying Product REST controller endpoints against MariaDB TestResource.
 */
@MicronautTest(environments = "test")
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class ProductControllerIntegrationTest {

    private static final String V1_PRODUCTS = "/v1/products";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String FIELD_SKU = "sku";
    private static final String FIELD_NAME = "name";

    @Test
    @DisplayName("POST /v1/products creates product and returns 201 Created")
    void createProductReturns201(final RequestSpecification spec) {
        final String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_SKU, sku,
                FIELD_NAME, "Test Mechanical Keyboard",
                "description", "RGB wireless keyboard",
                "price", 129.99,
                "currency", DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body(FIELD_SKU, equalTo(sku))
                .body(FIELD_NAME, equalTo("Test Mechanical Keyboard"))
                .body("status", equalTo("DRAFT"));
    }

    @Test
    @DisplayName("GET /v1/products/{id} returns 200 OK when product exists")
    void getProductByIdReturns200(final RequestSpecification spec) {
        final String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_SKU, sku,
                FIELD_NAME, "Gaming Mouse",
                "price", 59.99,
                "currency", DEFAULT_CURRENCY
        );

        final String id = given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given(spec)
                .when()
                .get(V1_PRODUCTS + "/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body(FIELD_SKU, equalTo(sku))
                .body(FIELD_NAME, equalTo("Gaming Mouse"));
    }

    @Test
    @DisplayName("GET /v1/products/{id} returns 404 when product not found")
    void getProductByIdReturns404(final RequestSpecification spec) {
        given(spec)
                .when()
                .get(V1_PRODUCTS + "/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("POST /v1/products with invalid SKU returns 400 Bad Request")
    void createProductInvalidSkuReturns400(final RequestSpecification spec) {
        final Map<String, Object> body = Map.of(
                FIELD_SKU, "invalid sku with spaces!",
                FIELD_NAME, "Invalid Product",
                "price", 10.00,
                "currency", DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(400);
    }
}
