package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.messaging;

import static org.mockito.Mockito.verify;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.SendOrderNotificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

@DisplayName("OrderCreatedNotificationListener Unit Tests")
class OrderCreatedNotificationListenerTest {

    @Test
    @DisplayName("Should forward OrderCreatedEvent orderId and traceId to SendOrderNotificationUseCase")
    void shouldForwardEventToUseCase() {
        final SendOrderNotificationUseCase useCase = Mockito.mock(SendOrderNotificationUseCase.class);
        final OrderCreatedNotificationListener listener = new OrderCreatedNotificationListener(useCase);

        final UUID orderId = UUID.randomUUID();
        final OrderCreatedEvent event = OrderCreatedEvent.create(orderId, 2500, "EUR", "trace-xyz");

        listener.onOrderCreated(event);

        verify(useCase).sendOrderNotification(orderId, "trace-xyz");
    }
}
