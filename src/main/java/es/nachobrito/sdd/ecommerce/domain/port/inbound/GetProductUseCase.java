package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port driving queries for products.
 */
public interface GetProductUseCase {

    /**
     * Finds a product by its unique identifier.
     *
     * @param id The product UUID
     * @return Optional containing the product if found
     */
    Optional<Product> getById(UUID id);

    /**
     * Lists all products in the catalog.
     *
     * @return List of products
     */
    List<Product> listAll();
}
