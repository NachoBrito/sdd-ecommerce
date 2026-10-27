package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

/**
 * Serdeable AMQP/RabbitMQ wire message DTO for order lifecycle events.
 *
 * @param eventId Unique event identifier
 * @param eventType Event type classification (e.g. order.created.v1, order.updated.v1, order.cancelled.v1)
 * @param orderId Order unique identifier
 * @param orderStatus Lifecycle status of the order (CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
 * @param totalAmountInCents Optional total amount in cents
 * @param currency Optional ISO currency code
 * @param timestamp Event emission timestamp in UTC
 * @param traceId Distributed tracing correlation identifier
 */
@Serdeable
public record OrderEventMessage(
        UUID eventId,
        String eventType,
        UUID orderId,
        String orderStatus,
        Integer totalAmountInCents,
        String currency,
        Instant timestamp,
        String traceId
) {
}
