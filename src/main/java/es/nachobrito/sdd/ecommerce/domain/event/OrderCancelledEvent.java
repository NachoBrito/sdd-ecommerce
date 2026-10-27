package es.nachobrito.sdd.ecommerce.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an Order aggregate is cancelled.
 *
 * @param eventId Unique event identifier
 * @param eventType Constant event type identifier
 * @param orderId Identifier of the cancelled order
 * @param reason Optional cancellation rationale
 * @param timestamp Event creation timestamp in UTC
 * @param traceId Correlation trace ID for distributed tracing
 */
public record OrderCancelledEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String reason,
        Instant timestamp,
        String traceId
) {

    public static final String EVENT_TYPE = "order.cancelled.v1";

    public OrderCancelledEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    /**
     * Factory method creating a new OrderCancelledEvent with default event type and timestamp.
     *
     * @param orderId Order unique identifier
     * @param reason Optional cancellation rationale
     * @param traceId Optional correlation trace identifier
     * @return New OrderCancelledEvent instance
     */
    public static OrderCancelledEvent create(
            final UUID orderId,
            final String reason,
            final String traceId
    ) {
        return new OrderCancelledEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                orderId,
                reason,
                Instant.now(),
                traceId
        );
    }
}
