package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port driving queries for retrieving a single order by ID.
 */
@FunctionalInterface
public interface GetOrderUseCase {

    /**
     * Finds an order by its unique identifier.
     *
     * @param id The order UUID
     * @return Optional containing the order if found, or empty if not found
     */
    Optional<Order> getOrder(UUID id);
}
