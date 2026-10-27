package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import es.nachobrito.sdd.ecommerce.domain.port.inbound.SendOrderNotificationUseCase;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging.dto.OrderEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

@DisplayName("OrderRabbitNotificationConsumer Unit Tests")
class OrderRabbitNotificationConsumerTest {

    private SendOrderNotificationUseCase sendOrderNotificationUseCase;
    private OrderRabbitNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        sendOrderNotificationUseCase = mock(SendOrderNotificationUseCase.class);
        consumer = new OrderRabbitNotificationConsumer(sendOrderNotificationUseCase);
    }

    @Test
    @DisplayName("Should invoke SendOrderNotificationUseCase when order event message is received")
    void shouldDelegateToUseCaseOnMessage() {
        final UUID orderId = UUID.randomUUID();
        final OrderEventMessage message = new OrderEventMessage(
                UUID.randomUUID(),
                "order.created.v1",
                orderId,
                "CREATED",
                1999,
                "EUR",
                Instant.now(),
                "trace-xyz"
        );

        consumer.receive(message);

        verify(sendOrderNotificationUseCase).sendOrderNotification(orderId, "trace-xyz");
    }
}
