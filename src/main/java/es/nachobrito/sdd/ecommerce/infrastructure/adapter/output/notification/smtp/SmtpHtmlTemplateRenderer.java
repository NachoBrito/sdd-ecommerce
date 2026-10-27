package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import es.nachobrito.sdd.ecommerce.domain.model.NotificationAddress;
import es.nachobrito.sdd.ecommerce.domain.model.NotificationLineItem;
import es.nachobrito.sdd.ecommerce.domain.model.OrderNotification;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Dedicated HTML email template renderer for the SMTP notification transport.
 */
@Singleton
public class SmtpHtmlTemplateRenderer {

    private static final String TEMPLATE_PATH = "templates/order-confirmation.html";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CREATED = "CREATED";

    private final String cachedTemplate;

    public SmtpHtmlTemplateRenderer() {
        this.cachedTemplate = loadTemplate(TEMPLATE_PATH);
    }

    /**
     * Renders the order confirmation HTML email body from the provided notification data.
     *
     * @param notification Notification data containing order details
     * @return Rendered HTML document string
     */
    public String render(final OrderNotification notification) {
        Objects.requireNonNull(notification, "notification must not be null");

        String rendered = cachedTemplate;
        final String status = notification.status() != null ? notification.status() : STATUS_CREATED;
        final String statusTitle;
        final String statusSubtitle;
        if (STATUS_CANCELLED.equalsIgnoreCase(status)) {
            statusTitle = "Order Cancelled";
            statusSubtitle = "Your order has been cancelled, " + escapeHtml(notification.customerName())
                    + ". Here are the details of the cancelled order.";
        } else if (STATUS_CREATED.equalsIgnoreCase(status)) {
            statusTitle = "Order Confirmation";
            statusSubtitle = "Thank you for your order, " + escapeHtml(notification.customerName())
                    + "! Here are your order details.";
        } else {
            statusTitle = "Order Updated";
            statusSubtitle = "Your order has been updated, " + escapeHtml(notification.customerName())
                    + ". Current status: " + escapeHtml(status) + ".";
        }

        rendered = rendered.replace("{{statusTitle}}", statusTitle);
        rendered = rendered.replace("{{statusSubtitle}}", statusSubtitle);
        rendered = rendered.replace("{{orderStatus}}", escapeHtml(status));
        rendered = rendered.replace("{{orderId}}", escapeHtml(notification.orderId().toString()));
        rendered = rendered.replace("{{orderDate}}", escapeHtml(notification.orderDate().toString()));
        rendered = rendered.replace("{{customerName}}", escapeHtml(notification.customerName()));
        rendered = rendered.replace("{{customerEmail}}", escapeHtml(notification.customerEmail()));
        rendered = rendered.replace("{{currency}}", escapeHtml(notification.currency()));
        rendered = rendered.replace("{{totalAmount}}", formatCents(notification.totalAmountInCents()));
        rendered = rendered.replace("{{deliveryAddress}}", renderAddress(notification.deliveryAddress()));
        rendered = rendered.replace("{{invoiceAddress}}", renderAddress(notification.invoiceAddress()));
        rendered = rendered.replace("{{orderLines}}", renderLines(notification));

        return rendered;
    }

    private String renderAddress(final NotificationAddress address) {
        if (address == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(512);
        sb.append("<strong>")
                .append(escapeHtml(address.name()))
                .append("</strong><br>")
                .append(escapeHtml(address.street()));
        if (address.complement() != null && !address.complement().isBlank()) {
            sb.append(", ").append(escapeHtml(address.complement()));
        }
        sb.append("<br>")
                .append(escapeHtml(address.city()))
                .append(", ")
                .append(escapeHtml(address.state()))
                .append(' ')
                .append(escapeHtml(address.zipCode()))
                .append("<br>")
                .append(escapeHtml(address.country()))
                .append("<br>Tel: ")
                .append(escapeHtml(address.telephone()));
        if (address.vatNumber() != null && !address.vatNumber().isBlank()) {
            sb.append("<br>VAT: ").append(escapeHtml(address.vatNumber()));
        }
        return sb.toString();
    }

    private String renderLines(final OrderNotification notification) {
        final StringBuilder sb = new StringBuilder(512);
        final String currency = escapeHtml(notification.currency());
        for (final NotificationLineItem line : notification.lines()) {
            sb.append("<tr><td>")
                    .append(escapeHtml(line.productId().toString()))
                    .append("</td><td style=\"text-align: center;\">")
                    .append(line.quantity())
                    .append("</td><td style=\"text-align: right;\">")
                    .append(formatCents(line.unitPriceInCents()))
                    .append(' ')
                    .append(currency)
                    .append("</td><td style=\"text-align: right;\">")
                    .append(formatCents(line.lineTotalInCents()))
                    .append(' ')
                    .append(currency)
                    .append("</td></tr>\n");
        }
        return sb.toString();
    }

    private String formatCents(final Integer cents) {
        if (cents == null) {
            return "0.00";
        }
        final double amount = cents / 100.0;
        return String.format(Locale.US, "%.2f", amount);
    }

    private String escapeHtml(final String input) {
        if (input == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder(Math.max(16, input.length()));
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            switch (c) {
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#39;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    private String loadTemplate(final String path) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Email template resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load email template: " + path, e);
        }
    }
}
