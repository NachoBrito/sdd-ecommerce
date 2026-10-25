package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.nachobrito.sdd.ecommerce.domain.model.NotificationAddress;
import es.nachobrito.sdd.ecommerce.domain.model.NotificationLineItem;
import es.nachobrito.sdd.ecommerce.domain.model.OrderNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@DisplayName("SmtpNotificationTransport Unit Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class SmtpNotificationTransportTest {

    private SmtpHtmlTemplateRenderer renderer;
    private SmtpSender smtpSender;
    private OrderNotification notification;
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        renderer = Mockito.mock(SmtpHtmlTemplateRenderer.class);
        smtpSender = Mockito.mock(SmtpSender.class);

        final NotificationAddress address = new NotificationAddress(
                "Jane", "123", "Street", null, "City", "State", "28001", "ES", null
        );
        final NotificationLineItem line = new NotificationLineItem(UUID.randomUUID(), 1, 1000, 1000);

        notification = new OrderNotification(
                orderId, "Jane", "jane@example.com", Instant.now(), "CREATED",
                1000, "EUR", address, address, List.of(line), "trace-1"
        );
    }

    @Test
    @DisplayName("Should deliver notification when transport is enabled")
    void shouldDeliverNotificationWhenEnabled() {
        final SmtpConfiguration config = new SmtpConfiguration(
                true, "localhost", 1025, "no-reply@ecommerce.com", null, null, false, false
        );
        when(renderer.render(notification)).thenReturn("<html>Sample</html>");

        final SmtpNotificationTransport transport = new SmtpNotificationTransport(config, renderer, smtpSender);

        assertEquals("SMTP", transport.transportName());
        assertTrue(transport.isEnabled());

        transport.send(notification);

        verify(renderer).render(notification);
        verify(smtpSender).sendMail(
                eq("no-reply@ecommerce.com"),
                eq("jane@example.com"),
                eq("Order Confirmation - " + orderId),
                eq("<html>Sample</html>")
        );
    }

    @Test
    @DisplayName("Should skip delivery when transport is disabled")
    void shouldSkipDeliveryWhenDisabled() {
        final SmtpConfiguration config = new SmtpConfiguration(
                false, "localhost", 1025, "no-reply@ecommerce.com", null, null, false, false
        );
        final SmtpNotificationTransport transport = new SmtpNotificationTransport(config, renderer, smtpSender);

        transport.send(notification);

        verify(renderer, never()).render(any());
        verify(smtpSender, never()).sendMail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should catch transmission exceptions without rethrowing (fault isolation)")
    void shouldIsolateTransmissionErrors() {
        final SmtpConfiguration config = new SmtpConfiguration(
                true, "localhost", 1025, "no-reply@ecommerce.com", null, null, false, false
        );
        when(renderer.render(notification)).thenReturn("<html>Sample</html>");
        doThrow(new SmtpDeliveryException("Connection refused"))
                .when(smtpSender).sendMail(anyString(), anyString(), anyString(), anyString());

        final SmtpNotificationTransport transport = new SmtpNotificationTransport(config, renderer, smtpSender);

        assertDoesNotThrow(() -> transport.send(notification));
    }
}
