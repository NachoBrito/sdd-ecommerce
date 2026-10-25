package es.nachobrito.sdd.ecommerce.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import es.nachobrito.sdd.ecommerce.domain.event.ProductCreatedEvent;
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
    void publishesEventSuccessfully() {
        final UUID productId = UUID.randomUUID();
        final String sku = "SKU-EVENT-1";
        final ProductCreatedEvent event = ProductCreatedEvent.create(productId, sku, "trace-123");

        eventPublisher.publish(event);

        assertAll(
                "Verify received domain event",
                () -> assertEquals(1, listener.getEvents().size(), "Should have received exactly 1 event"),
                () -> assertEquals(productId, listener.getEvents().getFirst().productId(), "Product ID must match"),
                () -> assertEquals(sku, listener.getEvents().getFirst().sku(), "SKU must match")
        );
    }

    /**
     * Domain event listener capturing domain events during tests.
     */
    @Singleton
    static class DomainEventCaptureListener {

        private final List<ProductCreatedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        void onProductCreated(final ProductCreatedEvent event) {
            events.add(event);
        }

        List<ProductCreatedEvent> getEvents() {
            return List.copyOf(events);
        }
    }
}
