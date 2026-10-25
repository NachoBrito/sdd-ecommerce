package es.nachobrito.sdd.ecommerce.domain.exception;

import java.io.Serial;
import java.util.UUID;

/**
 * Domain exception thrown when attempting to create an order with an ID that already exists.
 */
public class OrderAlreadyExistsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID orderId;

    /**
     * Constructs the exception with the offending order ID.
     *
     * @param orderId Existing order identifier
     */
    public OrderAlreadyExistsException(final UUID orderId) {
        super("Order with ID '" + orderId + "' already exists");
        this.orderId = orderId;
    }

    /**
     * Retrieves the conflicting order ID.
     *
     * @return Order UUID
     */
    public UUID getOrderId() {
        return orderId;
    }
}
