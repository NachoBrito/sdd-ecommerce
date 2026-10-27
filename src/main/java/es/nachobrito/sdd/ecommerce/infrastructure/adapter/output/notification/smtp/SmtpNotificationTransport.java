package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import es.nachobrito.sdd.ecommerce.domain.model.OrderNotification;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.NotificationTransportPort;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Notification transport implementation for delivering order confirmations via SMTP HTML email.
 */
@Singleton
public class SmtpNotificationTransport implements NotificationTransportPort {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpNotificationTransport.class);
    private static final String TRANSPORT_NAME = "SMTP";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CREATED = "CREATED";

    private final SmtpConfiguration config;
    private final SmtpHtmlTemplateRenderer renderer;
    private final SmtpSender smtpSender;

    public SmtpNotificationTransport(
            final SmtpConfiguration config,
            final SmtpHtmlTemplateRenderer renderer,
            final SmtpSender smtpSender
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
        this.smtpSender = Objects.requireNonNull(smtpSender, "smtpSender must not be null");
    }

    @Override
    public String transportName() {
        return TRANSPORT_NAME;
    }

    @Override
    public boolean isEnabled() {
        return config.enabled();
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void send(final OrderNotification notification) {
        Objects.requireNonNull(notification, "notification must not be null");

        if (!isEnabled()) {
            LOG.debug("SMTP transport is disabled. Skipping notification for orderId={}", notification.orderId());
            return;
        }

        LOG.info("Preparing SMTP notification for orderId={}, recipient={}",
                notification.orderId(), notification.customerEmail());

        try {
            final String subject;
            if (STATUS_CANCELLED.equalsIgnoreCase(notification.status())) {
                subject = "Order Cancelled - " + notification.orderId();
            } else if (STATUS_CREATED.equalsIgnoreCase(notification.status())) {
                subject = "Order Confirmation - " + notification.orderId();
            } else {
                subject = "Order Updated - " + notification.orderId();
            }
            final String htmlContent = renderer.render(notification);

            smtpSender.sendMail(config.fromAddress(), notification.customerEmail(), subject, htmlContent);

            LOG.info("Successfully dispatched SMTP email to {} for orderId={}",
                    notification.customerEmail(), notification.orderId());
        } catch (Exception e) {
            LOG.error("Failed to dispatch SMTP email to {} for orderId={}: {}",
                    notification.customerEmail(), notification.orderId(), e.getMessage(), e);
        }
    }
}
