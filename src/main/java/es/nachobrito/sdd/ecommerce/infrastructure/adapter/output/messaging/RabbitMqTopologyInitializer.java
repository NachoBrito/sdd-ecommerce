package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Declares the RabbitMQ topology (topic exchange, notifications queue, and bindings) upon channel creation.
 */
@Singleton
public class RabbitMqTopologyInitializer extends ChannelInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMqTopologyInitializer.class);

    public static final String ORDERS_EXCHANGE = "ecommerce.orders.exchange";
    public static final String NOTIFICATIONS_QUEUE = "ecommerce.orders.notifications.queue";
    public static final String ORDERS_ROUTING_KEY_PATTERN = "order.*";

    @Override
    public void initialize(final Channel channel, final String name) throws IOException {
        LOG.info("Initializing RabbitMQ topology on channel pool: {}", name);
        channel.exchangeDeclare(ORDERS_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        channel.queueDeclare(NOTIFICATIONS_QUEUE, true, false, false, null);
        channel.queueBind(NOTIFICATIONS_QUEUE, ORDERS_EXCHANGE, ORDERS_ROUTING_KEY_PATTERN);
        LOG.info("RabbitMQ topology initialized: exchange={}, queue={}, binding={}",
                ORDERS_EXCHANGE, NOTIFICATIONS_QUEUE, ORDERS_ROUTING_KEY_PATTERN);
    }
}
