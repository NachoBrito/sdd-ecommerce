package es.nachobrito.sdd.ecommerce.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCancelledEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderUpdatedEvent;
import es.nachobrito.sdd.ecommerce.domain.exception.OrderAlreadyExistsException;
import es.nachobrito.sdd.ecommerce.domain.exception.OrderNotFoundException;
import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.EventPublisherPort;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DisplayName("OrderService Unit Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
class OrderServiceTest {

    private OrderRepositoryPort orderRepository;
    private EventPublisherPort eventPublisher;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        orderService = new OrderService(orderRepository, eventPublisher);
    }

    private Order createSampleOrder(final UUID id, final OrderStatus status) {
        final DeliveryAddress delivery = new DeliveryAddress(
                "Jane Doe", "+34600112233", "Gran Via 12", "Apt 4B",
                "Madrid", "Madrid", "28013", "Spain"
        );
        final InvoiceAddress invoice = new InvoiceAddress(
                "Acme Corp", "ESB12345678", "+34911223344", "Paseo de la Castellana 100",
                null, "Madrid", "Madrid", "28046", "Spain"
        );
        final OrderLine line = new OrderLine(UUID.randomUUID(), 2, 1000, 2000);
        return new Order(
                id, "Jane Doe", "jane@example.com", java.time.Instant.now(),
                status, 2000, "EUR", delivery, invoice, List.of(line)
        );
    }

    @Test
    @DisplayName("Should successfully create order and publish OrderCreatedEvent")
    void shouldCreateOrderAndPublishEvent() {
        final UUID id = UUID.randomUUID();
        when(orderRepository.existsById(id)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final DeliveryAddress delivery = new DeliveryAddress(
                "Jane Doe", "+34600112233", "Gran Via 12", "Apt 4B",
                "Madrid", "Madrid", "28013", "Spain"
        );
        final InvoiceAddress invoice = new InvoiceAddress(
                "Acme Corp", "ESB12345678", "+34911223344", "Paseo de la Castellana 100",
                null, "Madrid", "Madrid", "28046", "Spain"
        );
        final OrderLine line = new OrderLine(UUID.randomUUID(), 1, 1000, 1000);

        final Order result = orderService.createOrder(
                id, "Jane Doe", "jane@example.com", delivery, invoice,
                List.of(line), 1000, "EUR"
        );

        final ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        assertAll(
                "Verify created order and event",
                () -> assertNotNull(result, "Result must not be null"),
                () -> assertEquals(id, result.id(), "Order ID must match"),
                () -> assertEquals(OrderStatus.CREATED, result.status(), "Status must be CREATED"),
                () -> assertEquals(id, captor.getValue().orderId(), "Event order ID must match")
        );
    }

    @Test
    @DisplayName("Should throw OrderAlreadyExistsException when creating order with existing ID")
    void shouldThrowExceptionWhenOrderAlreadyExists() {
        final UUID id = UUID.randomUUID();
        when(orderRepository.existsById(id)).thenReturn(true);

        assertThrows(OrderAlreadyExistsException.class, () -> orderService.createOrder(
                id, "Jane", "jane@example.com", null, null, List.of(), 100, "EUR"
        ), "Creating order with existing ID must throw OrderAlreadyExistsException");
    }

    @Test
    @DisplayName("Should successfully update order status and publish OrderUpdatedEvent")
    void shouldUpdateOrderStatusAndPublishEvent() {
        final UUID id = UUID.randomUUID();
        final Order existing = createSampleOrder(id, OrderStatus.CREATED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final Order updated = orderService.updateOrderStatus(id, OrderStatus.CONFIRMED);

        final ArgumentCaptor<OrderUpdatedEvent> captor = ArgumentCaptor.forClass(OrderUpdatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertAll(
                "Verify OrderUpdatedEvent",
                () -> assertEquals(OrderStatus.CONFIRMED, updated.status(), "Status must be CONFIRMED"),
                () -> assertEquals(id, captor.getValue().orderId(), "Order ID must match"),
                () -> assertEquals(OrderStatus.CONFIRMED, captor.getValue().status(), "Event status must match")
        );
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when updating non-existent order")
    void shouldThrowWhenUpdatingNonExistentOrder() {
        final UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrderStatus(id, OrderStatus.SHIPPED),
                "Updating non-existent order must throw OrderNotFoundException");
    }

    @Test
    @DisplayName("Should successfully cancel order and publish OrderCancelledEvent")
    void shouldCancelOrderAndPublishEvent() {
        final UUID id = UUID.randomUUID();
        final Order existing = createSampleOrder(id, OrderStatus.CREATED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final Order cancelled = orderService.cancelOrder(id, "Customer request");

        final ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertAll(
                "Verify OrderCancelledEvent",
                () -> assertEquals(OrderStatus.CANCELLED, cancelled.status(), "Status must be CANCELLED"),
                () -> assertEquals(id, captor.getValue().orderId(), "Order ID must match"),
                () -> assertEquals("Customer request", captor.getValue().reason(), "Reason must match")
        );
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when cancelling non-existent order")
    void shouldThrowWhenCancellingNonExistentOrder() {
        final UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(id, "Not found"),
                "Cancelling non-existent order must throw OrderNotFoundException");
    }
}
