package es.nachobrito.sdd.ecommerce.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an Order aggregate is created.
 *
 * @param eventId Unique event identifier
 * @param eventType Constant event type identifier
 * @param orderId Identifier of the created order
 * @param totalAmountInCents Total monetary amount of the order in cents
 * @param currency ISO-4217 currency code
 * @param timestamp Event creation timestamp in UTC
 * @param traceId Correlation trace ID for distributed tracing
 */
public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        Integer totalAmountInCents,
        String currency,
        Instant timestamp,
        String traceId
) {

    public static final String EVENT_TYPE = "order.created.v1";

    public OrderCreatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(totalAmountInCents, "totalAmountInCents must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    /**
     * Factory method creating a new OrderCreatedEvent with default event type and timestamp.
     *
     * @param orderId Order unique identifier
     * @param totalAmountInCents Order total amount in cents
     * @param currency Currency code
     * @param traceId Optional correlation trace identifier
     * @return New OrderCreatedEvent instance
     */
    public static OrderCreatedEvent create(
            final UUID orderId,
            final Integer totalAmountInCents,
            final String currency,
            final String traceId
    ) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                orderId,
                totalAmountInCents,
                currency,
                Instant.now(),
                traceId
        );
    }
}
