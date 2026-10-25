package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import java.util.UUID;

/**
 * Inbound port driving the dispatch of order notifications when an order is created.
 */
@FunctionalInterface
public interface SendOrderNotificationUseCase {

    /**
     * Dispatches order notifications across all active transports for the specified order.
     *
     * @param orderId Unique identifier of the placed order
     * @param traceId Correlation trace identifier for distributed tracing
     */
    void sendOrderNotification(UUID orderId, String traceId);
}
