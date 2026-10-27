package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.messaging;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.SendOrderNotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Inbound event listener consuming OrderCreatedEvent.
 *
 * @deprecated Replaced by {@link OrderRabbitNotificationConsumer} for asynchronous RabbitMQ event processing.
 */
@Deprecated(since = "0.0.5", forRemoval = true)
public class OrderCreatedNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(OrderCreatedNotificationListener.class);

    private final SendOrderNotificationUseCase sendOrderNotificationUseCase;

    /**
     * Constructs the deprecated listener.
     *
     * @param sendOrderNotificationUseCase Inbound use case for notifications
     */
    public OrderCreatedNotificationListener(final SendOrderNotificationUseCase sendOrderNotificationUseCase) {
        this.sendOrderNotificationUseCase = Objects.requireNonNull(
                sendOrderNotificationUseCase, "sendOrderNotificationUseCase must not be null"
        );
    }

    /**
     * Handles order creation events synchronously.
     *
     * @param event The published order creation domain event
     */
    public void onOrderCreated(final OrderCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        LOG.info("Deprecated synchronous OrderCreatedNotificationListener invoked: eventId={}, orderId={}",
                event.eventId(), event.orderId());
        sendOrderNotificationUseCase.sendOrderNotification(event.orderId(), event.traceId());
    }
}
