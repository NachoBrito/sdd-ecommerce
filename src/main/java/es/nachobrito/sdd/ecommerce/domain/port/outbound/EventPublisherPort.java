package es.nachobrito.sdd.ecommerce.domain.port.outbound;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.ProductCreatedEvent;

/**
 * Outbound port for publishing domain events.
 */
public interface EventPublisherPort {

    /**
     * Publishes a ProductCreatedEvent to the event bus.
     *
     * @param event The product created event
     */
    void publish(ProductCreatedEvent event);

    /**
     * Publishes an OrderCreatedEvent to the event bus.
     *
     * @param event The order created event
     */
    void publish(OrderCreatedEvent event);
}
