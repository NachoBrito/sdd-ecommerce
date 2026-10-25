package es.nachobrito.sdd.ecommerce.domain.port.outbound;

import es.nachobrito.sdd.ecommerce.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Order aggregate persistence operations.
 */
public interface OrderRepositoryPort {

    /**
     * Persists an Order aggregate and its line items.
     *
     * @param order The order aggregate to save
     * @return The persisted order
     */
    Order save(Order order);

    /**
     * Finds an Order aggregate by its unique identifier.
     *
     * @param id The order identifier
     * @return Optional containing the order if found, or empty if not found
     */
    Optional<Order> findById(UUID id);

    /**
     * Checks if an order exists with the given unique identifier.
     *
     * @param id The order identifier
     * @return true if an order with the ID exists, false otherwise
     */
    boolean existsById(UUID id);

    /**
     * Retrieves all Order aggregates.
     *
     * @return List of all persisted orders
     */
    List<Order> findAll();
}
