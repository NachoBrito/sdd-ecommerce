package es.nachobrito.sdd.ecommerce.infrastructure;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
    private static final String FIELD_ID = "id";
    private static final String FIELD_SKU = "sku";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_PRICE_IN_CENTS = "priceInCents";
    private static final String FIELD_CURRENCY = "currency";
    private static final String PREFIX_SKU = "SKU-";

    @Test
    @DisplayName("POST /v1/products creates product with client-generated ID and priceInCents and returns 201 Created")
    void createProductReturns201(final RequestSpecification spec) {
        final UUID id = UUID.randomUUID();
        final String sku = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, sku,
                FIELD_NAME, "Test Mechanical Keyboard",
                "description", "RGB wireless keyboard",
                FIELD_PRICE_IN_CENTS, 12_999,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(201)
                .body(FIELD_ID, equalTo(id.toString()))
                .body(FIELD_SKU, equalTo(sku))
                .body(FIELD_NAME, equalTo("Test Mechanical Keyboard"))
                .body(FIELD_PRICE_IN_CENTS, equalTo(12_999))
                .body("status", equalTo("DRAFT"));
    }

    @Test
    @DisplayName("GET /v1/products/{id} returns 200 OK when product exists")
    void getProductByIdReturns200(final RequestSpecification spec) {
        final UUID id = UUID.randomUUID();
        final String sku = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, sku,
                FIELD_NAME, "Gaming Mouse",
                FIELD_PRICE_IN_CENTS, 5_999,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(201);

        given(spec)
                .when()
                .get(V1_PRODUCTS + "/" + id)
                .then()
                .statusCode(200)
                .body(FIELD_ID, equalTo(id.toString()))
                .body(FIELD_SKU, equalTo(sku))
                .body(FIELD_NAME, equalTo("Gaming Mouse"))
                .body(FIELD_PRICE_IN_CENTS, equalTo(5_999));
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
    @DisplayName("POST /v1/products missing client ID returns 400 Bad Request")
    void createProductMissingIdReturns400(final RequestSpecification spec) {
        final String sku = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_SKU, sku,
                FIELD_NAME, "No ID Product",
                FIELD_PRICE_IN_CENTS, 1_999,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /v1/products with duplicate ID returns 409 Conflict")
    void createProductDuplicateIdReturns409(final RequestSpecification spec) {
        final UUID id = UUID.randomUUID();
        final String sku1 = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body1 = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, sku1,
                FIELD_NAME, "First Product",
                FIELD_PRICE_IN_CENTS, 2_500,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body1)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(201);

        final String sku2 = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body2 = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, sku2,
                FIELD_NAME, "Conflicting Product",
                FIELD_PRICE_IN_CENTS, 3_000,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body2)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(409)
                .body("status", equalTo(409));
    }

    @Test
    @DisplayName("POST /v1/products with invalid or non-positive priceInCents returns 400 Bad Request")
    void createProductInvalidPriceInCentsReturns400(final RequestSpecification spec) {
        final UUID id = UUID.randomUUID();
        final String sku = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, sku,
                FIELD_NAME, "Zero Price Product",
                FIELD_PRICE_IN_CENTS, 0,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /v1/products with invalid SKU returns 400 Bad Request")
    void createProductInvalidSkuReturns400(final RequestSpecification spec) {
        final UUID id = UUID.randomUUID();
        final Map<String, Object> body = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, "invalid sku with spaces!",
                FIELD_NAME, "Invalid Product",
                FIELD_PRICE_IN_CENTS, 1_000,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /v1/products lists all products with 200 OK")
    void listProductsReturns200(final RequestSpecification spec) {
        final UUID id = UUID.randomUUID();
        final String sku = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, sku,
                FIELD_NAME, "Listable Product",
                FIELD_PRICE_IN_CENTS, 2_000,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(201);

        given(spec)
                .when()
                .get(V1_PRODUCTS)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("GET /v1/products/{id} returns 200 when exists and 404 when not found")
    void getProductByIdReturns200And404(final RequestSpecification spec) {
        final UUID id = UUID.randomUUID();
        final String sku = PREFIX_SKU + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        final Map<String, Object> body = Map.of(
                FIELD_ID, id.toString(),
                FIELD_SKU, sku,
                FIELD_NAME, "Specific Product",
                FIELD_PRICE_IN_CENTS, 3_500,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_PRODUCTS)
                .then()
                .statusCode(201);

        given(spec)
                .when()
                .get(V1_PRODUCTS + "/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("sku", equalTo(sku))
                .body("name", equalTo("Specific Product"))
                .body("priceInCents", equalTo(3500))
                .body("status", notNullValue());

        given(spec)
                .when()
                .get(V1_PRODUCTS + "/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}

