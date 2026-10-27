package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.messaging;

import es.nachobrito.sdd.ecommerce.domain.port.inbound.SendOrderNotificationUseCase;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging.dto.OrderEventMessage;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * RabbitMQ message consumer listening on the notifications queue to dispatch order notifications.
 */
@RabbitListener
public class OrderRabbitNotificationConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OrderRabbitNotificationConsumer.class);

    private final SendOrderNotificationUseCase sendOrderNotificationUseCase;

    /**
     * Constructs the RabbitMQ notification consumer with constructor injection.
     *
     * @param sendOrderNotificationUseCase Inbound use case for triggering customer notifications
     */
    public OrderRabbitNotificationConsumer(final SendOrderNotificationUseCase sendOrderNotificationUseCase) {
        this.sendOrderNotificationUseCase = Objects.requireNonNull(
                sendOrderNotificationUseCase, "sendOrderNotificationUseCase must not be null"
        );
    }

    /**
     * Consumes order lifecycle event messages from RabbitMQ and dispatches customer notifications.
     *
     * @param message Deserialized order event message payload
     */
    @Queue("ecommerce.orders.notifications.queue")
    public void receive(final OrderEventMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        LOG.info("Received order event message from RabbitMQ: eventId={}, eventType={}, orderId={}, status={}",
                message.eventId(), message.eventType(), message.orderId(), message.orderStatus());

        sendOrderNotificationUseCase.sendOrderNotification(message.orderId(), message.traceId());
    }
}
