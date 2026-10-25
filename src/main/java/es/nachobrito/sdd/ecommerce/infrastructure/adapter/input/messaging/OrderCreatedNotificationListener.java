package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.messaging;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.SendOrderNotificationUseCase;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Inbound event listener consuming OrderCreatedEvent and triggering customer order notifications.
 */
@Singleton
public class OrderCreatedNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(OrderCreatedNotificationListener.class);

    private final SendOrderNotificationUseCase sendOrderNotificationUseCase;

    public OrderCreatedNotificationListener(final SendOrderNotificationUseCase sendOrderNotificationUseCase) {
        this.sendOrderNotificationUseCase = Objects.requireNonNull(
                sendOrderNotificationUseCase, "sendOrderNotificationUseCase must not be null"
        );
    }

    /**
     * Handles order creation events by dispatching notifications to all configured transports.
     *
     * @param event The published order creation domain event
     */
    @EventListener
    public void onOrderCreated(final OrderCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        LOG.info("Received OrderCreatedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());
        sendOrderNotificationUseCase.sendOrderNotification(event.orderId(), event.traceId());
    }
}
