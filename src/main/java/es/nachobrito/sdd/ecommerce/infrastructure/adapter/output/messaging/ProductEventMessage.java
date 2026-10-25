package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

/**
 * Serdeable AMQP/Event bus wire message DTO for ProductCreatedEvent.
 *
 * @param eventId Unique event identifier
 * @param eventType Event type classification
 * @param productId Product unique identifier
 * @param sku Product stock keeping unit
 * @param timestamp Event emission timestamp
 * @param traceId Distributed tracing correlation identifier
 */
@Serdeable
public record ProductEventMessage(
        UUID eventId,
        String eventType,
        UUID productId,
        String sku,
        Instant timestamp,
        String traceId
) {
}
