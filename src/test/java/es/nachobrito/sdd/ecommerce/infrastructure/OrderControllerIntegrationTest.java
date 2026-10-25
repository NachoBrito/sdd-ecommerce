package es.nachobrito.sdd.ecommerce.infrastructure;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integration test verifying Order REST controller endpoints against MariaDB TestResource.
 */
@MicronautTest(environments = "test")
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class OrderControllerIntegrationTest {

    private static final String V1_ORDERS = "/v1/orders";
    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String DEFAULT_CITY = "Madrid";
    private static final String DEFAULT_COUNTRY = "Spain";
    private static final String DEFAULT_CUSTOMER_NAME = "John Doe";
    private static final String DEFAULT_CUSTOMER_EMAIL = "john.doe@example.com";
    private static final String FIELD_CUSTOMER_NAME = "customerName";
    private static final String FIELD_CUSTOMER_EMAIL = "customerEmail";
    private static final String FIELD_LINES = "lines";
    private static final String FIELD_TOTAL_AMOUNT = "totalAmountInCents";
    private static final String FIELD_CURRENCY = "currency";

    private Map<String, Object> createDeliveryAddressPayload() {
        final Map<String, Object> map = new HashMap<>();
        map.put("name", "Alice Smith");
        map.put("telephone", "+34600112233");
        map.put("street", "Gran Via 42");
        map.put("complement", "Flat 3B");
        map.put("city", DEFAULT_CITY);
        map.put("state", DEFAULT_CITY);
        map.put("zipCode", "28013");
        map.put("country", DEFAULT_COUNTRY);
        return map;
    }

    private Map<String, Object> createInvoiceAddressPayload() {
        final Map<String, Object> map = new HashMap<>();
        map.put("name", "Alice Smith Consulting");
        map.put("vatNumber", "ESA12345678");
        map.put("telephone", "+34911223344");
        map.put("street", "Gran Via 42");
        map.put("complement", null);
        map.put("city", DEFAULT_CITY);
        map.put("state", DEFAULT_CITY);
        map.put("zipCode", "28013");
        map.put("country", DEFAULT_COUNTRY);
        return map;
    }

    private Map<String, Object> createOrderLinePayload(final UUID productId, final int qty, final int unitPrice) {
        return Map.of(
                "productId", productId.toString(),
                "quantity", qty,
                "unitPriceInCents", unitPrice,
                "lineTotalInCents", qty * unitPrice
        );
    }

    private Map<String, Object> createOrderPayload(
            final UUID orderId,
            final List<Map<String, Object>> lines,
            final int totalAmountInCents
    ) {
        return Map.of(
                "id", orderId.toString(),
                FIELD_CUSTOMER_NAME, DEFAULT_CUSTOMER_NAME,
                FIELD_CUSTOMER_EMAIL, DEFAULT_CUSTOMER_EMAIL,
                "deliveryAddress", createDeliveryAddressPayload(),
                "invoiceAddress", createInvoiceAddressPayload(),
                FIELD_LINES, lines,
                FIELD_TOTAL_AMOUNT, totalAmountInCents,
                FIELD_CURRENCY, DEFAULT_CURRENCY
        );
    }

    @Test
    @DisplayName("POST /v1/orders creates order with valid details and returns 201 Created")
    void createOrderReturns201(final RequestSpecification spec) {
        final UUID orderId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final Map<String, Object> line = createOrderLinePayload(productId, 2, 1999);
        final Map<String, Object> body = createOrderPayload(orderId, List.of(line), 3998);

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_ORDERS)
                .then()
                .statusCode(201)
                .body("id", equalTo(orderId.toString()))
                .body("status", equalTo("CREATED"))
                .body(FIELD_CUSTOMER_NAME, equalTo(DEFAULT_CUSTOMER_NAME))
                .body(FIELD_CUSTOMER_EMAIL, equalTo(DEFAULT_CUSTOMER_EMAIL))
                .body(FIELD_TOTAL_AMOUNT, equalTo(3998))
                .body(FIELD_CURRENCY, equalTo(DEFAULT_CURRENCY))
                .body(FIELD_LINES, hasSize(1))
                .body("deliveryAddress.name", equalTo("Alice Smith"))
                .body("invoiceAddress.vatNumber", equalTo("ESA12345678"));
    }

    @Test
    @DisplayName("POST /v1/orders with duplicate ID returns 409 Conflict")
    void createOrderDuplicateIdReturns409(final RequestSpecification spec) {
        final UUID orderId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final Map<String, Object> line = createOrderLinePayload(productId, 1, 2500);
        final Map<String, Object> body = createOrderPayload(orderId, List.of(line), 2500);

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_ORDERS)
                .then()
                .statusCode(201);

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_ORDERS)
                .then()
                .statusCode(409)
                .body("status", equalTo(409));
    }

    @Test
    @DisplayName("POST /v1/orders with mismatched total amount returns 400 Bad Request")
    void createOrderMismatchedTotalReturns400(final RequestSpecification spec) {
        final UUID orderId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final Map<String, Object> line = createOrderLinePayload(productId, 2, 1000); // 2000
        final Map<String, Object> body = createOrderPayload(orderId, List.of(line), 2500); // mismatch

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_ORDERS)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /v1/orders with empty lines list returns 400 Bad Request")
    void createOrderEmptyLinesReturns400(final RequestSpecification spec) {
        final UUID orderId = UUID.randomUUID();
        final Map<String, Object> body = createOrderPayload(orderId, List.of(), 1000);

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_ORDERS)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /v1/orders/{id} returns 200 OK when order exists")
    void getOrderByIdReturns200(final RequestSpecification spec) {
        final UUID orderId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final Map<String, Object> line = createOrderLinePayload(productId, 1, 5000);
        final Map<String, Object> body = createOrderPayload(orderId, List.of(line), 5000);

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_ORDERS)
                .then()
                .statusCode(201);

        given(spec)
                .when()
                .get(V1_ORDERS + "/" + orderId)
                .then()
                .statusCode(200)
                .body("id", equalTo(orderId.toString()))
                .body("status", equalTo("CREATED"))
                .body(FIELD_CUSTOMER_NAME, equalTo(DEFAULT_CUSTOMER_NAME))
                .body(FIELD_CUSTOMER_EMAIL, equalTo(DEFAULT_CUSTOMER_EMAIL))
                .body(FIELD_TOTAL_AMOUNT, equalTo(5000))
                .body(FIELD_CURRENCY, equalTo(DEFAULT_CURRENCY));
    }

    @Test
    @DisplayName("GET /v1/orders/{id} returns 404 when order does not exist")
    void getOrderByIdReturns404(final RequestSpecification spec) {
        given(spec)
                .when()
                .get(V1_ORDERS + "/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GET /v1/orders lists all persisted orders with 200 OK")
    void listOrdersReturns200(final RequestSpecification spec) {
        final UUID orderId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final Map<String, Object> line = createOrderLinePayload(productId, 1, 1200);
        final Map<String, Object> body = createOrderPayload(orderId, List.of(line), 1200);

        given(spec)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(V1_ORDERS)
                .then()
                .statusCode(201);

        given(spec)
                .when()
                .get(V1_ORDERS)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }
}
