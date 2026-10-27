package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;

import java.util.UUID;

/**
 * Inbound port for updating an order's lifecycle status.
 */
@FunctionalInterface
public interface UpdateOrderStatusUseCase {

    /**
     * Updates an order's lifecycle status.
     *
     * @param orderId Unique identifier of the order
     * @param newStatus New lifecycle status
     * @return Updated Order aggregate
     */
    Order updateOrderStatus(UUID orderId, OrderStatus newStatus);
}
