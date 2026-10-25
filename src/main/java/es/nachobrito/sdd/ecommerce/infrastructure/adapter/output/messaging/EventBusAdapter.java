package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.ProductCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.EventPublisherPort;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Event bus adapter publishing domain events to the internal application event bus.
 */
@Singleton
public class EventBusAdapter implements EventPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(EventBusAdapter.class);

    private final ApplicationEventPublisher<Object> eventPublisher;

    /**
     * Constructs the event bus adapter with constructor injection.
     *
     * @param eventPublisher Internal Micronaut application event publisher
     */
    public EventBusAdapter(final ApplicationEventPublisher<Object> eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public void publish(final ProductCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        LOG.info("Publishing domain event: type={}, eventId={}, productId={}",
                event.eventType(), event.eventId(), event.productId());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(final OrderCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        LOG.info("Publishing domain event: type={}, eventId={}, orderId={}",
                event.eventType(), event.eventId(), event.orderId());
        eventPublisher.publishEvent(event);
    }
}
