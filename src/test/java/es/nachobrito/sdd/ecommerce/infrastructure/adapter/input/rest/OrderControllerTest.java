package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;
import es.nachobrito.sdd.ecommerce.domain.exception.OrderAlreadyExistsException;
import es.nachobrito.sdd.ecommerce.domain.exception.OrderNotFoundException;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.CreateOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.GetOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.ListOrdersUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

@DisplayName("OrderController Unit Tests")
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class OrderControllerTest {

    private OrderController controller;

    @BeforeEach
    void setUp() {
        final CreateOrderUseCase createOrderUseCase = Mockito.mock(CreateOrderUseCase.class);
        final GetOrderUseCase getOrderUseCase = Mockito.mock(GetOrderUseCase.class);
        final ListOrdersUseCase listOrdersUseCase = Mockito.mock(ListOrdersUseCase.class);
        controller = new OrderController(createOrderUseCase, getOrderUseCase, listOrdersUseCase);
    }

    @Test
    @DisplayName("handleOrderNotFound should return 404 with error body")
    void shouldHandleOrderNotFound() {
        final UUID id = UUID.randomUUID();
        final OrderNotFoundException ex = new OrderNotFoundException(id);

        final HttpResponse<Map<String, Object>> response = controller.handleOrderNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "Status must be NOT_FOUND");
        assertNotNull(response.body(), "Response body must not be null");
        assertEquals(ex.getMessage(), response.body().get("message"), "Error message must match");
        assertEquals(404, response.body().get("status"), "Status code must be 404");
    }

    @Test
    @DisplayName("handleOrderAlreadyExists should return 409 with error body")
    void shouldHandleOrderAlreadyExists() {
        final UUID id = UUID.randomUUID();
        final OrderAlreadyExistsException ex = new OrderAlreadyExistsException(id);

        final HttpResponse<Map<String, Object>> response = controller.handleOrderAlreadyExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatus(), "Status must be CONFLICT");
        assertNotNull(response.body(), "Response body must not be null");
        assertEquals(ex.getMessage(), response.body().get("message"), "Error message must match");
        assertEquals(409, response.body().get("status"), "Status code must be 409");
    }

    @Test
    @DisplayName("handleInvalidOrder should return 400 with error body")
    void shouldHandleInvalidOrder() {
        final InvalidOrderException ex = new InvalidOrderException("Invalid order details");

        final HttpResponse<Map<String, Object>> response = controller.handleInvalidOrder(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(), "Status must be BAD_REQUEST");
        assertNotNull(response.body(), "Response body must not be null");
        assertEquals("Invalid order details", response.body().get("message"), "Error message must match");
        assertEquals(400, response.body().get("status"), "Status code must be 400");
    }
}
