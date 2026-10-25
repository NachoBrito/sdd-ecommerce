package es.nachobrito.sdd.ecommerce.domain.port.outbound;

import es.nachobrito.sdd.ecommerce.domain.model.OrderNotification;

/**
 * Outbound driven port for notification transports delivering order notifications.
 */
public interface NotificationTransportPort {

    /**
     * Unique identifier for this transport medium (e.g., "SMTP", "SMS").
     *
     * @return Transport name string
     */
    String transportName();

    /**
     * Indicates whether this transport is currently active and configured to send messages.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Renders this transport's dedicated template and delivers the order notification.
     *
     * @param notification Notification data payload containing order details
     */
    void send(OrderNotification notification);
}
