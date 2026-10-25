package es.nachobrito.sdd.ecommerce.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.GetOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.NotificationTransportPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DisplayName("NotificationService Application Service Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
class NotificationServiceTest {

    private GetOrderUseCase getOrderUseCase;
    private NotificationTransportPort transportA;
    private NotificationTransportPort transportB;
    private final UUID orderId = UUID.randomUUID();
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        getOrderUseCase = Mockito.mock(GetOrderUseCase.class);
        transportA = Mockito.mock(NotificationTransportPort.class);
        transportB = Mockito.mock(NotificationTransportPort.class);

        when(transportA.transportName()).thenReturn("SMTP");
        when(transportB.transportName()).thenReturn("SMS");

        final DeliveryAddress delivery = new DeliveryAddress(
                "Jane Doe", "+34611223344", "Street 1", "Apt 4", "Madrid", "Madrid", "28001", "Spain"
        );
        final InvoiceAddress invoice = new InvoiceAddress(
                "Jane Doe", "ES12345678", "+34611223344", "Street 1", null, "Madrid", "Madrid", "28001", "Spain"
        );
        final OrderLine line = new OrderLine(UUID.randomUUID(), 2, 1000, 2000);

        sampleOrder = new Order(
                orderId, "Jane Doe", "jane@example.com", Instant.now(), OrderStatus.CREATED,
                2000, "EUR", delivery, invoice, List.of(line)
        );
    }

    @Test
    @DisplayName("Should dispatch notification to all enabled transports")
    void shouldDispatchToAllEnabledTransports() {
        when(getOrderUseCase.getOrder(orderId)).thenReturn(Optional.of(sampleOrder));
        when(transportA.isEnabled()).thenReturn(true);
        when(transportB.isEnabled()).thenReturn(true);

        final NotificationService service = new NotificationService(getOrderUseCase, List.of(transportA, transportB));
        service.sendOrderNotification(orderId, "trace-123");

        verify(transportA).send(any());
        verify(transportB).send(any());
    }

    @Test
    @DisplayName("Should skip disabled transports while executing enabled ones")
    void shouldSkipDisabledTransports() {
        when(getOrderUseCase.getOrder(orderId)).thenReturn(Optional.of(sampleOrder));
        when(transportA.isEnabled()).thenReturn(true);
        when(transportB.isEnabled()).thenReturn(false);

        final NotificationService service = new NotificationService(getOrderUseCase, List.of(transportA, transportB));
        service.sendOrderNotification(orderId, "trace-123");

        verify(transportA).send(any());
        verify(transportB, never()).send(any());
    }

    @Test
    @DisplayName("Should skip notification when order is not found")
    void shouldSkipWhenOrderNotFound() {
        when(getOrderUseCase.getOrder(orderId)).thenReturn(Optional.empty());

        final NotificationService service = new NotificationService(getOrderUseCase, List.of(transportA));
        service.sendOrderNotification(orderId, "trace-123");

        verify(transportA, never()).send(any());
    }

    @Test
    @DisplayName("Should handle empty transport list gracefully without throwing")
    void shouldHandleEmptyTransportsGracefully() {
        when(getOrderUseCase.getOrder(orderId)).thenReturn(Optional.of(sampleOrder));

        final NotificationService service = new NotificationService(getOrderUseCase, List.of());
        assertDoesNotThrow(() -> service.sendOrderNotification(orderId, "trace-123"));
    }

    @Test
    @DisplayName("Should continue to next transport if one transport throws an unhandled exception")
    void shouldContinueWhenOneTransportFails() {
        when(getOrderUseCase.getOrder(orderId)).thenReturn(Optional.of(sampleOrder));
        when(transportA.isEnabled()).thenReturn(true);
        when(transportB.isEnabled()).thenReturn(true);
        doThrow(new RuntimeException("Crash in transport A")).when(transportA).send(any());

        final NotificationService service = new NotificationService(getOrderUseCase, List.of(transportA, transportB));
        assertDoesNotThrow(() -> service.sendOrderNotification(orderId, "trace-123"));

        verify(transportA).send(any());
        verify(transportB).send(any());
    }
}
