package es.nachobrito.sdd.ecommerce.domain.exception;

import java.io.Serial;
import java.util.UUID;

/**
 * Domain exception thrown when an order cannot be found by its unique identifier.
 */
public class OrderNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID orderId;

    /**
     * Constructs the exception with the requested order ID.
     *
     * @param orderId Requested order identifier
     */
    public OrderNotFoundException(final UUID orderId) {
        super("Order with ID '" + orderId + "' was not found");
        this.orderId = orderId;
    }

    /**
     * Retrieves the missing order ID.
     *
     * @return Order UUID
     */
    public UUID getOrderId() {
        return orderId;
    }
}
