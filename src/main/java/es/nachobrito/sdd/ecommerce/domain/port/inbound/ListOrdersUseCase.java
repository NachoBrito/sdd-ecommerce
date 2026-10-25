package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.Order;

import java.util.List;

/**
 * Inbound port driving queries for listing all orders.
 */
@FunctionalInterface
public interface ListOrdersUseCase {

    /**
     * Lists all orders.
     *
     * @return List of all persisted orders
     */
    List<Order> listOrders();
}
