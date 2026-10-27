package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import es.nachobrito.sdd.ecommerce.domain.event.OrderCancelledEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.event.OrderUpdatedEvent;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.messaging.dto.OrderEventMessage;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

@DisplayName("EventBusAdapter Unit Tests")
@SuppressWarnings({"unchecked", "PMD.UnitTestContainsTooManyAsserts"})
class EventBusAdapterTest {

    private ApplicationEventPublisher<Object> applicationEventPublisher;
    private RabbitOrderEventClient rabbitOrderEventClient;
    private EventBusAdapter eventBusAdapter;

    @BeforeEach
    void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        rabbitOrderEventClient = mock(RabbitOrderEventClient.class);
        eventBusAdapter = new EventBusAdapter(applicationEventPublisher, rabbitOrderEventClient);
    }

    @Test
    @DisplayName("Should publish OrderCreatedEvent to RabbitMQ client and application event publisher")
    void shouldPublishOrderCreatedEvent() {
        final UUID orderId = UUID.randomUUID();
        final OrderCreatedEvent event = OrderCreatedEvent.create(orderId, 4500, "EUR", "trace-123");

        eventBusAdapter.publish(event);

        final ArgumentCaptor<OrderEventMessage> captor = ArgumentCaptor.forClass(OrderEventMessage.class);
        verify(rabbitOrderEventClient).sendOrderCreated(captor.capture());
        verify(applicationEventPublisher).publishEvent(event);

        final OrderEventMessage message = captor.getValue();
        assertAll(
                "Verify OrderCreated message",
                () -> assertNotNull(message, "Message must not be null"),
                () -> assertEquals(event.eventId(), message.eventId(), "Event ID must match"),
                () -> assertEquals("order.created.v1", message.eventType(), "Event type must match"),
                () -> assertEquals(orderId, message.orderId(), "Order ID must match"),
                () -> assertEquals("CREATED", message.orderStatus(), "Status must be CREATED"),
                () -> assertEquals(4500, message.totalAmountInCents(), "Total amount must match"),
                () -> assertEquals("EUR", message.currency(), "Currency must match"),
                () -> assertEquals("trace-123", message.traceId(), "Trace ID must match")
        );
    }

    @Test
    @DisplayName("Should publish OrderUpdatedEvent to RabbitMQ client and application event publisher")
    void shouldPublishOrderUpdatedEvent() {
        final UUID orderId = UUID.randomUUID();
        final OrderUpdatedEvent event = OrderUpdatedEvent.create(orderId, OrderStatus.SHIPPED, "trace-456");

        eventBusAdapter.publish(event);

        final ArgumentCaptor<OrderEventMessage> captor = ArgumentCaptor.forClass(OrderEventMessage.class);
        verify(rabbitOrderEventClient).sendOrderUpdated(captor.capture());
        verify(applicationEventPublisher).publishEvent(event);

        final OrderEventMessage message = captor.getValue();
        assertAll(
                "Verify OrderUpdated message",
                () -> assertNotNull(message, "Message must not be null"),
                () -> assertEquals(event.eventId(), message.eventId(), "Event ID must match"),
                () -> assertEquals("order.updated.v1", message.eventType(), "Event type must match"),
                () -> assertEquals(orderId, message.orderId(), "Order ID must match"),
                () -> assertEquals("SHIPPED", message.orderStatus(), "Status must be SHIPPED"),
                () -> assertEquals("trace-456", message.traceId(), "Trace ID must match")
        );
    }

    @Test
    @DisplayName("Should publish OrderCancelledEvent to RabbitMQ client and application event publisher")
    void shouldPublishOrderCancelledEvent() {
        final UUID orderId = UUID.randomUUID();
        final OrderCancelledEvent event = OrderCancelledEvent.create(orderId, "Customer requested", "trace-789");

        eventBusAdapter.publish(event);

        final ArgumentCaptor<OrderEventMessage> captor = ArgumentCaptor.forClass(OrderEventMessage.class);
        verify(rabbitOrderEventClient).sendOrderCancelled(captor.capture());
        verify(applicationEventPublisher).publishEvent(event);

        final OrderEventMessage message = captor.getValue();
        assertAll(
                "Verify OrderCancelled message",
                () -> assertNotNull(message, "Message must not be null"),
                () -> assertEquals(event.eventId(), message.eventId(), "Event ID must match"),
                () -> assertEquals("order.cancelled.v1", message.eventType(), "Event type must match"),
                () -> assertEquals(orderId, message.orderId(), "Order ID must match"),
                () -> assertEquals("CANCELLED", message.orderStatus(), "Status must be CANCELLED"),
                () -> assertEquals("trace-789", message.traceId(), "Trace ID must match")
        );
    }
}
