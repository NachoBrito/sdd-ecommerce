package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;
import es.nachobrito.sdd.ecommerce.domain.exception.OrderAlreadyExistsException;
import es.nachobrito.sdd.ecommerce.domain.exception.OrderNotFoundException;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.CreateOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.GetOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.ListOrdersUseCase;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto.CreateOrderRequest;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto.OrderResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller implementing Order endpoints per OpenAPI contract.
 */
@Controller("/v1/orders")
@ExecuteOn(TaskExecutors.BLOCKING)
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;

    /**
     * Constructs the controller with explicit constructor injection.
     *
     * @param createOrderUseCase Inbound port for creating orders
     * @param getOrderUseCase Inbound port for retrieving orders by ID
     * @param listOrdersUseCase Inbound port for listing orders
     */
    public OrderController(
            final CreateOrderUseCase createOrderUseCase,
            final GetOrderUseCase getOrderUseCase,
            final ListOrdersUseCase listOrdersUseCase
    ) {
        this.createOrderUseCase = Objects.requireNonNull(createOrderUseCase, "createOrderUseCase must not be null");
        this.getOrderUseCase = Objects.requireNonNull(getOrderUseCase, "getOrderUseCase must not be null");
        this.listOrdersUseCase = Objects.requireNonNull(listOrdersUseCase, "listOrdersUseCase must not be null");
    }

    /**
     * Creates a new order.
     *
     * @param request Validated create order request payload
     * @return HTTP 201 Created with OrderResponse
     */
    @Post
    public HttpResponse<OrderResponse> createOrder(@Body @Valid final CreateOrderRequest request) {
        final Order order = createOrderUseCase.createOrder(
                request.id(),
                request.customerName(),
                request.customerEmail(),
                request.toDeliveryAddress(),
                request.toInvoiceAddress(),
                request.toOrderLines(),
                request.totalAmountInCents(),
                request.currency()
        );

        return HttpResponse.created(OrderResponse.fromDomain(order));
    }

    /**
     * Retrieves an order by unique identifier.
     *
     * @param id Order UUID
     * @return HTTP 200 OK with OrderResponse or HTTP 404 Not Found
     */
    @Get("/{id}")
    public HttpResponse<OrderResponse> getOrderById(@PathVariable final UUID id) {
        return getOrderUseCase.getOrder(id)
                .map(OrderResponse::fromDomain)
                .map(HttpResponse::ok)
                .orElseGet(HttpResponse::notFound);
    }

    /**
     * Lists all orders.
     *
     * @return List of OrderResponse DTOs
     */
    @Get
    public List<OrderResponse> listOrders() {
        return listOrdersUseCase.listOrders().stream()
                .map(OrderResponse::fromDomain)
                .toList();
    }

    /**
     * Handles duplicate order ID conflict when creating an order.
     *
     * @param ex Conflict exception thrown from domain/service layer
     * @return HTTP 409 Conflict response
     */
    @Error(exception = OrderAlreadyExistsException.class)
    public HttpResponse<Map<String, Object>> handleOrderAlreadyExists(final OrderAlreadyExistsException ex) {
        return HttpResponse.status(HttpStatus.CONFLICT).body(Map.of(
                "message", ex.getMessage(),
                "status", HttpStatus.CONFLICT.getCode()
        ));
    }

    /**
     * Handles domain validation exceptions as bad requests.
     *
     * @param ex Validation exception thrown from domain layer
     * @return HTTP 400 Bad Request response
     */
    @Error(exception = InvalidOrderException.class)
    public HttpResponse<Map<String, Object>> handleInvalidOrder(final InvalidOrderException ex) {
        return HttpResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", ex.getMessage(),
                "status", HttpStatus.BAD_REQUEST.getCode()
        ));
    }

    /**
     * Handles order not found exceptions.
     *
     * @param ex Not found exception thrown from domain/service layer
     * @return HTTP 404 Not Found response
     */
    @Error(exception = OrderNotFoundException.class)
    public HttpResponse<Map<String, Object>> handleOrderNotFound(final OrderNotFoundException ex) {
        return HttpResponse.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", ex.getMessage(),
                "status", HttpStatus.NOT_FOUND.getCode()
        ));
    }
}
