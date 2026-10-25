package es.nachobrito.sdd.ecommerce.domain.exception;

import java.io.Serial;

/**
 * Domain exception thrown when an order fails domain validation invariants.
 */
public class InvalidOrderException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs the exception with an explanatory validation error message.
     *
     * @param message Explanatory message
     */
    public InvalidOrderException(final String message) {
        super(message);
    }
}
