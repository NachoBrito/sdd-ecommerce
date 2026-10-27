package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging;

import es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging.dto.OrderEventMessage;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;

/**
 * Declarative Micronaut RabbitMQ client publishing order lifecycle events to the orders topic exchange.
 */
@RabbitClient("ecommerce.orders.exchange")
public interface RabbitOrderEventClient {

    /**
     * Publishes an order created event message with routing key 'order.created'.
     *
     * @param message The order event message payload
     */
    @Binding("order.created")
    void sendOrderCreated(OrderEventMessage message);

    /**
     * Publishes an order updated event message with routing key 'order.updated'.
     *
     * @param message The order event message payload
     */
    @Binding("order.updated")
    void sendOrderUpdated(OrderEventMessage message);

    /**
     * Publishes an order cancelled event message with routing key 'order.cancelled'.
     *
     * @param message The order event message payload
     */
    @Binding("order.cancelled")
    void sendOrderCancelled(OrderEventMessage message);
}
