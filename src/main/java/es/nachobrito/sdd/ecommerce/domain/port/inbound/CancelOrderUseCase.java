package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.Order;

import java.util.UUID;

/**
 * Inbound port for cancelling an existing order.
 */
@FunctionalInterface
public interface CancelOrderUseCase {

    /**
     * Cancels an order.
     *
     * @param orderId Unique identifier of the order to cancel
     * @param reason Optional cancellation rationale
     * @return Cancelled Order aggregate
     */
    Order cancelOrder(UUID orderId, String reason);
}
