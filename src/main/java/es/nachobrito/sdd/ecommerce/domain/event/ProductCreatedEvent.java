package es.nachobrito.sdd.ecommerce.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a Product aggregate is created.
 *
 * @param eventId Unique event identifier
 * @param eventType Constant event type identifier
 * @param productId Identifier of the created product
 * @param sku Stock keeping unit of the created product
 * @param timestamp Event creation timestamp in UTC
 * @param traceId Correlation trace ID for distributed tracing
 */
public record ProductCreatedEvent(
        UUID eventId,
        String eventType,
        UUID productId,
        String sku,
        Instant timestamp,
        String traceId
) {

    public static final String EVENT_TYPE = "product.created.v1";

    public ProductCreatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    /**
     * Factory method creating a new ProductCreatedEvent with default event type and timestamp.
     *
     * @param productId Product unique identifier
     * @param sku Stock keeping unit
     * @param traceId Optional correlation trace identifier
     * @return New ProductCreatedEvent instance
     */
    public static ProductCreatedEvent create(final UUID productId, final String sku, final String traceId) {
        return new ProductCreatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                productId,
                sku,
                Instant.now(),
                traceId
        );
    }
}
