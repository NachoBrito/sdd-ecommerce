package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.nachobrito.sdd.ecommerce.domain.model.NotificationAddress;
import es.nachobrito.sdd.ecommerce.domain.model.NotificationLineItem;
import es.nachobrito.sdd.ecommerce.domain.model.OrderNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@DisplayName("SmtpHtmlTemplateRenderer Unit Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class SmtpHtmlTemplateRendererTest {

    private final SmtpHtmlTemplateRenderer renderer = new SmtpHtmlTemplateRenderer();

    @Test
    @DisplayName("Should successfully render HTML template with all order fields replaced")
    void shouldRenderHtmlWithOrderDetails() {
        final UUID orderId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        final UUID productId = UUID.fromString("7e59676e-82b2-4d2c-880c-7871b6d05f32");
        final Instant now = Instant.parse("2026-10-25T14:30:00Z");

        final NotificationAddress delivery = new NotificationAddress(
                "Jane Doe", "+34611223344", "Paseo de la Castellana 100", "Floor 4",
                "Madrid", "Madrid", "28046", "Spain", null
        );
        final NotificationAddress invoice = new NotificationAddress(
                "Jane Doe Corp", "+34611223344", "Paseo de la Castellana 100", null,
                "Madrid", "Madrid", "28046", "Spain", "ESB12345678"
        );
        final NotificationLineItem line = new NotificationLineItem(productId, 2, 1999, 3998);

        final OrderNotification notification = new OrderNotification(
                orderId, "Jane Doe", "jane.doe@example.com", now, "CREATED",
                3998, "EUR", delivery, invoice, List.of(line), "trace-abc"
        );

        final String html = renderer.render(notification);

        assertNotNull(html);
        assertTrue(html.contains("123e4567-e89b-12d3-a456-426614174000"));
        assertTrue(html.contains("Jane Doe"));
        assertTrue(html.contains("Paseo de la Castellana 100"));
        assertTrue(html.contains("Floor 4"));
        assertTrue(html.contains("ESB12345678"));
        assertTrue(html.contains("7e59676e-82b2-4d2c-880c-7871b6d05f32"));
        assertTrue(html.contains("39.98 EUR"));
        assertFalse(html.contains("{{orderId}}"));
        assertFalse(html.contains("{{customerName}}"));
        assertFalse(html.contains("{{orderLines}}"));
    }

    @Test
    @DisplayName("Should escape HTML characters to prevent XSS injection")
    void shouldEscapeHtmlInDynamicFields() {
        final UUID orderId = UUID.randomUUID();
        final NotificationAddress delivery = new NotificationAddress(
                "<script>alert('xss')</script>", "+34600000000", "Street & Co <test>",
                null, "City", "State", "28001", "Spain", null
        );
        final NotificationAddress invoice = new NotificationAddress(
                "Company \"quoted\"", "+34600000000", "Street", null, "City", "State", "28001", "Spain", null
        );
        final NotificationLineItem line = new NotificationLineItem(UUID.randomUUID(), 1, 1000, 1000);

        final OrderNotification notification = new OrderNotification(
                orderId, "<b>Hacker</b>", "hacker@example.com", Instant.now(), "CREATED",
                1000, "EUR", delivery, invoice, List.of(line), null
        );

        final String html = renderer.render(notification);

        assertFalse(html.contains("<script>alert('xss')</script>"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        assertFalse(html.contains("<b>Hacker</b>"));
        assertTrue(html.contains("&lt;b&gt;Hacker&lt;/b&gt;"));
        assertTrue(html.contains("Street &amp; Co &lt;test&gt;"));
        assertTrue(html.contains("Company &quot;quoted&quot;"));
    }

    @Test
    @DisplayName("Should handle null complement cleanly without printing 'null'")
    void shouldOmitNullComplementInAddressFormatting() {
        final UUID orderId = UUID.randomUUID();
        final NotificationAddress addressWithoutComplement = new NotificationAddress(
                "Bob", "+34600000000", "Main Street 1", null, "Barcelona", "Barcelona", "08001", "Spain", null
        );
        final NotificationLineItem line = new NotificationLineItem(UUID.randomUUID(), 1, 500, 500);

        final OrderNotification notification = new OrderNotification(
                orderId, "Bob", "bob@example.com", Instant.now(), "CREATED",
                500, "EUR", addressWithoutComplement, addressWithoutComplement, List.of(line), null
        );

        final String html = renderer.render(notification);

        assertNotNull(html);
        assertTrue(html.contains("Main Street 1<br>"));
        assertFalse(html.contains("null"));
    }
}
