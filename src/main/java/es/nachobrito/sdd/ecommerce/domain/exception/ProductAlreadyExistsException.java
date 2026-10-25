package es.nachobrito.sdd.ecommerce.domain.exception;

import java.io.Serial;
import java.util.UUID;

/**
 * Domain exception thrown when attempting to create a product with an ID that already exists.
 */
public class ProductAlreadyExistsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID productId;

    /**
     * Constructs the exception with the offending product ID.
     *
     * @param productId Existing product identifier
     */
    public ProductAlreadyExistsException(final UUID productId) {
        super("Product with ID '" + productId + "' already exists");
        this.productId = productId;
    }

    /**
     * Retrieves the conflicting product ID.
     *
     * @return Product UUID
     */
    public UUID getProductId() {
        return productId;
    }
}
