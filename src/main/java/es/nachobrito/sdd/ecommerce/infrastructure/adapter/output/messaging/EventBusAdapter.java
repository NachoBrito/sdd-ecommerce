package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCancelledEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderUpdatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.ProductCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.EventPublisherPort;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging.dto.OrderEventMessage;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Event bus adapter publishing domain events to RabbitMQ and the internal application event bus.
 */
@Singleton
public class EventBusAdapter implements EventPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(EventBusAdapter.class);
    private static final String EVENT_MUST_NOT_BE_NULL = "event must not be null";

    private final ApplicationEventPublisher<Object> eventPublisher;
    private final RabbitOrderEventClient rabbitOrderEventClient;

    /**
     * Constructs the event bus adapter with constructor injection.
     *
     * @param eventPublisher Internal Micronaut application event publisher
     * @param rabbitOrderEventClient Declarative RabbitMQ client for order events
     */
    public EventBusAdapter(
            final ApplicationEventPublisher<Object> eventPublisher,
            final RabbitOrderEventClient rabbitOrderEventClient
    ) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.rabbitOrderEventClient = Objects.requireNonNull(
                rabbitOrderEventClient, "rabbitOrderEventClient must not be null"
        );
    }

    @Override
    public void publish(final ProductCreatedEvent event) {
        Objects.requireNonNull(event, EVENT_MUST_NOT_BE_NULL);
        LOG.info("Publishing domain event: type={}, eventId={}, productId={}",
                event.eventType(), event.eventId(), event.productId());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(final OrderCreatedEvent event) {
        Objects.requireNonNull(event, EVENT_MUST_NOT_BE_NULL);
        LOG.info("Publishing OrderCreatedEvent to RabbitMQ: eventId={}, orderId={}",
                event.eventId(), event.orderId());

        final OrderEventMessage message = new OrderEventMessage(
                event.eventId(),
                event.eventType(),
                event.orderId(),
                "CREATED",
                event.totalAmountInCents(),
                event.currency(),
                event.timestamp(),
                event.traceId()
        );
        rabbitOrderEventClient.sendOrderCreated(message);
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(final OrderUpdatedEvent event) {
        Objects.requireNonNull(event, EVENT_MUST_NOT_BE_NULL);
        LOG.info("Publishing OrderUpdatedEvent to RabbitMQ: eventId={}, orderId={}, status={}",
                event.eventId(), event.orderId(), event.status());

        final OrderEventMessage message = new OrderEventMessage(
                event.eventId(),
                event.eventType(),
                event.orderId(),
                event.status().name(),
                null,
                null,
                event.timestamp(),
                event.traceId()
        );
        rabbitOrderEventClient.sendOrderUpdated(message);
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(final OrderCancelledEvent event) {
        Objects.requireNonNull(event, EVENT_MUST_NOT_BE_NULL);
        LOG.info("Publishing OrderCancelledEvent to RabbitMQ: eventId={}, orderId={}",
                event.eventId(), event.orderId());

        final OrderEventMessage message = new OrderEventMessage(
                event.eventId(),
                event.eventType(),
                event.orderId(),
                "CANCELLED",
                null,
                null,
                event.timestamp(),
                event.traceId()
        );
        rabbitOrderEventClient.sendOrderCancelled(message);
        eventPublisher.publishEvent(event);
    }
}
