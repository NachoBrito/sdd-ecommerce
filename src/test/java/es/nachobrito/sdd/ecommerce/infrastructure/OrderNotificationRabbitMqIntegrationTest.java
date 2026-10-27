package es.nachobrito.sdd.ecommerce.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import es.nachobrito.sdd.ecommerce.domain.model.OrderNotification;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.CancelOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.CreateOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.UpdateOrderStatusUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.NotificationTransportPort;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * End-to-end integration test verifying asynchronous order notification processing
 * via RabbitMQ message broker spun up automatically by Micronaut TestResources.
 */
@MicronautTest(environments = "test", transactional = false)
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.UnitTestContainsTooManyAsserts"})
class OrderNotificationRabbitMqIntegrationTest {

    private static final long TIMEOUT_MS = 10_000L;
    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String DEFAULT_CITY = "Madrid";
    private static final String DEFAULT_COUNTRY = "Spain";

    @Inject
    private CreateOrderUseCase createOrderUseCase;

    @Inject
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Inject
    private CancelOrderUseCase cancelOrderUseCase;

    @Inject
    private CapturingNotificationTransport capturingTransport;

    private DeliveryAddress createDeliveryAddress() {
        return new DeliveryAddress(
                "Jane Doe",
                "+34611223344",
                "Calle Mayor 10",
                "3A",
                DEFAULT_CITY,
                DEFAULT_CITY,
                "28013",
                DEFAULT_COUNTRY
        );
    }

    private InvoiceAddress createInvoiceAddress() {
        return new InvoiceAddress(
                "Jane Doe Corp",
                "ESB12345678",
                "+34911223344",
                "Calle Mayor 10",
                null,
                DEFAULT_CITY,
                DEFAULT_CITY,
                "28013",
                DEFAULT_COUNTRY
        );
    }

    private void waitUntil(final Supplier<Boolean> condition, final long timeoutMs) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.get()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Condition was not met within " + timeoutMs + " milliseconds");
            }
            Thread.sleep(100);
        }
    }

    @Test
    @DisplayName("Should publish OrderCreatedEvent to RabbitMQ and dispatch async notification")
    void shouldProcessOrderCreatedNotificationViaRabbitMq() throws InterruptedException {
        final UUID orderId = UUID.randomUUID();
        final OrderLine line = new OrderLine(UUID.randomUUID(), 2, 2000, 4000);

        final Order created = createOrderUseCase.createOrder(
                orderId,
                "Jane Doe",
                "jane.rabbit@example.com",
                createDeliveryAddress(),
                createInvoiceAddress(),
                List.of(line),
                4000,
                DEFAULT_CURRENCY
        );
        assertEquals(OrderStatus.CREATED, created.status(), "Order status must be CREATED");

        waitUntil(() -> capturingTransport.getCaptured().stream()
                .anyMatch(n -> n.orderId().equals(orderId)), TIMEOUT_MS);

        final OrderNotification notification = capturingTransport.getCaptured().stream()
                .filter(n -> n.orderId().equals(orderId))
                .findFirst()
                .orElseThrow();

        assertAll(
                "Verify OrderCreated async notification",
                () -> assertEquals(orderId, notification.orderId(), "Order ID must match"),
                () -> assertEquals("jane.rabbit@example.com", notification.customerEmail(), "Customer email must match"),
                () -> assertEquals("Jane Doe", notification.customerName(), "Customer name must match"),
                () -> assertEquals("CREATED", notification.status(), "Order status must be CREATED"),
                () -> assertEquals(4000, notification.totalAmountInCents(), "Total amount must match"),
                () -> assertEquals("EUR", notification.currency(), "Currency must match")
        );
    }

    @Test
    @DisplayName("Should publish OrderUpdatedEvent to RabbitMQ and dispatch async notification")
    void shouldProcessOrderUpdatedNotificationViaRabbitMq() throws InterruptedException {
        final UUID orderId = UUID.randomUUID();
        final OrderLine line = new OrderLine(UUID.randomUUID(), 1, 1500, 1500);

        createOrderUseCase.createOrder(
                orderId,
                "Alice Smith",
                "alice.rabbit@example.com",
                createDeliveryAddress(),
                createInvoiceAddress(),
                List.of(line),
                1500,
                DEFAULT_CURRENCY
        );

        waitUntil(() -> capturingTransport.getCaptured().stream()
                .anyMatch(n -> n.orderId().equals(orderId)), TIMEOUT_MS);

        final int initialCount = capturingTransport.getCaptured().size();

        updateOrderStatusUseCase.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

        waitUntil(() -> capturingTransport.getCaptured().size() > initialCount, TIMEOUT_MS);

        final boolean hasUpdatedNotification = capturingTransport.getCaptured().stream()
                .anyMatch(n -> n.orderId().equals(orderId) && "CONFIRMED".equals(n.status()));

        assertTrue(hasUpdatedNotification, "Must receive CONFIRMED status notification via RabbitMQ");
    }

    @Test
    @DisplayName("Should publish OrderCancelledEvent to RabbitMQ and dispatch async notification")
    void shouldProcessOrderCancelledNotificationViaRabbitMq() throws InterruptedException {
        final UUID orderId = UUID.randomUUID();
        final OrderLine line = new OrderLine(UUID.randomUUID(), 1, 3000, 3000);

        createOrderUseCase.createOrder(
                orderId,
                "Bob Jones",
                "bob.rabbit@example.com",
                createDeliveryAddress(),
                createInvoiceAddress(),
                List.of(line),
                3000,
                DEFAULT_CURRENCY
        );

        waitUntil(() -> capturingTransport.getCaptured().stream()
                .anyMatch(n -> n.orderId().equals(orderId)), TIMEOUT_MS);

        final int initialCount = capturingTransport.getCaptured().size();

        cancelOrderUseCase.cancelOrder(orderId, "Customer change of mind");

        waitUntil(() -> capturingTransport.getCaptured().size() > initialCount, TIMEOUT_MS);

        final boolean hasCancelledNotification = capturingTransport.getCaptured().stream()
                .anyMatch(n -> n.orderId().equals(orderId) && "CANCELLED".equals(n.status()));

        assertTrue(hasCancelledNotification, "Must receive CANCELLED status notification via RabbitMQ");
    }

    /**
     * Test transport capturing order notifications dispatched by NotificationService.
     */
    @Singleton
    static class CapturingNotificationTransport implements NotificationTransportPort {

        private final List<OrderNotification> captured = new CopyOnWriteArrayList<>();

        @Override
        public String transportName() {
            return "CAPTURING";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void send(final OrderNotification notification) {
            captured.add(notification);
        }

        List<OrderNotification> getCaptured() {
            return List.copyOf(captured);
        }
    }
}
