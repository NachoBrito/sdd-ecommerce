package es.nachobrito.sdd.ecommerce.domain.event;

import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an Order aggregate status or attributes are updated.
 *
 * @param eventId Unique event identifier
 * @param eventType Constant event type identifier
 * @param orderId Identifier of the updated order
 * @param status New lifecycle status of the order
 * @param timestamp Event creation timestamp in UTC
 * @param traceId Correlation trace ID for distributed tracing
 */
public record OrderUpdatedEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        OrderStatus status,
        Instant timestamp,
        String traceId
) {

    public static final String EVENT_TYPE = "order.updated.v1";

    public OrderUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    /**
     * Factory method creating a new OrderUpdatedEvent with default event type and timestamp.
     *
     * @param orderId Order unique identifier
     * @param status New lifecycle status
     * @param traceId Optional correlation trace identifier
     * @return New OrderUpdatedEvent instance
     */
    public static OrderUpdatedEvent create(
            final UUID orderId,
            final OrderStatus status,
            final String traceId
    ) {
        return new OrderUpdatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                orderId,
                status,
                Instant.now(),
                traceId
        );
    }
}
