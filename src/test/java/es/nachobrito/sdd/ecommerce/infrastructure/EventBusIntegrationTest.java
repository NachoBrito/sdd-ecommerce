package es.nachobrito.sdd.ecommerce.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCancelledEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderUpdatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.ProductCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.EventPublisherPort;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Integration test verifying internal domain event dispatching.
 */
@MicronautTest(environments = "test")
class EventBusIntegrationTest {

    @Inject
    private EventPublisherPort eventPublisher;

    @Inject
    private DomainEventCaptureListener listener;

    @Test
    @DisplayName("Publishing ProductCreatedEvent notifies internal event listeners")
    void publishesProductCreatedEventSuccessfully() {
        final UUID productId = UUID.randomUUID();
        final String sku = "SKU-EVENT-1";
        final ProductCreatedEvent event = ProductCreatedEvent.create(productId, sku, "trace-123");

        eventPublisher.publish(event);

        assertAll(
                "Verify received domain event",
                () -> assertEquals(1, listener.getProductEvents().size(), "Should have received exactly 1 event"),
                () -> assertEquals(productId, listener.getProductEvents().getFirst().productId(), "Product ID must match"),
                () -> assertEquals(sku, listener.getProductEvents().getFirst().sku(), "SKU must match")
        );
    }

    @Test
    @DisplayName("Publishing OrderCreatedEvent notifies internal event listeners and RabbitMQ")
    void publishesOrderCreatedEventSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final OrderCreatedEvent event = OrderCreatedEvent.create(
                orderId,
                5000,
                "EUR",
                "trace-order-created"
        );

        eventPublisher.publish(event);

        assertAll(
                "Verify OrderCreatedEvent received",
                () -> assertEquals(1, listener.getOrderCreatedEvents().size(), "Should receive 1 event"),
                () -> assertEquals(orderId, listener.getOrderCreatedEvents().getFirst().orderId(), "Order ID must match")
        );
    }

    @Test
    @DisplayName("Publishing OrderUpdatedEvent notifies internal event listeners and RabbitMQ")
    void publishesOrderUpdatedEventSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final OrderUpdatedEvent event = OrderUpdatedEvent.create(
                orderId,
                OrderStatus.CONFIRMED,
                "trace-order-updated"
        );

        eventPublisher.publish(event);

        assertAll(
                "Verify OrderUpdatedEvent received",
                () -> assertEquals(1, listener.getOrderUpdatedEvents().size(), "Should receive 1 event"),
                () -> assertEquals(orderId, listener.getOrderUpdatedEvents().getFirst().orderId(), "Order ID must match"),
                () -> assertEquals(OrderStatus.CONFIRMED, listener.getOrderUpdatedEvents().getFirst().status(), "Status must match")
        );
    }

    @Test
    @DisplayName("Publishing OrderCancelledEvent notifies internal event listeners and RabbitMQ")
    void publishesOrderCancelledEventSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final OrderCancelledEvent event = OrderCancelledEvent.create(
                orderId,
                "Customer request",
                "trace-order-cancelled"
        );

        eventPublisher.publish(event);

        assertAll(
                "Verify OrderCancelledEvent received",
                () -> assertEquals(1, listener.getOrderCancelledEvents().size(), "Should receive 1 event"),
                () -> assertEquals(orderId, listener.getOrderCancelledEvents().getFirst().orderId(), "Order ID must match")
        );
    }

    /**
     * Domain event listener capturing domain events during tests.
     */
    @Singleton
    static class DomainEventCaptureListener {

        private final List<ProductCreatedEvent> productEvents = new CopyOnWriteArrayList<>();
        private final List<OrderCreatedEvent> orderCreatedEvents = new CopyOnWriteArrayList<>();
        private final List<OrderUpdatedEvent> orderUpdatedEvents = new CopyOnWriteArrayList<>();
        private final List<OrderCancelledEvent> orderCancelledEvents = new CopyOnWriteArrayList<>();

        @EventListener
        void onProductCreated(final ProductCreatedEvent event) {
            productEvents.add(event);
        }

        @EventListener
        void onOrderCreated(final OrderCreatedEvent event) {
            orderCreatedEvents.add(event);
        }

        @EventListener
        void onOrderUpdated(final OrderUpdatedEvent event) {
            orderUpdatedEvents.add(event);
        }

        @EventListener
        void onOrderCancelled(final OrderCancelledEvent event) {
            orderCancelledEvents.add(event);
        }

        List<ProductCreatedEvent> getProductEvents() {
            return List.copyOf(productEvents);
        }

        List<OrderCreatedEvent> getOrderCreatedEvents() {
            return List.copyOf(orderCreatedEvents);
        }

        List<OrderUpdatedEvent> getOrderUpdatedEvents() {
            return List.copyOf(orderUpdatedEvents);
        }

        List<OrderCancelledEvent> getOrderCancelledEvents() {
            return List.copyOf(orderCancelledEvents);
        }
    }
}
