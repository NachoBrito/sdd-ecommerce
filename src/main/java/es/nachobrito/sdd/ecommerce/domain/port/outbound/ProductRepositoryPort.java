package es.nachobrito.sdd.ecommerce.domain.port.outbound;

import es.nachobrito.sdd.ecommerce.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Product aggregate persistence operations.
 */
public interface ProductRepositoryPort {

    /**
     * Persists or updates a Product aggregate.
     *
     * @param product The product aggregate to save
     * @return The persisted product
     */
    Product save(Product product);

    /**
     * Finds a Product aggregate by unique identifier.
     *
     * @param id The product identifier
     * @return Optional containing the product if found
     */
    Optional<Product> findById(UUID id);

    /**
     * Finds a Product aggregate by its unique SKU.
     *
     * @param sku The stock keeping unit
     * @return Optional containing the product if found
     */
    Optional<Product> findBySku(String sku);

    /**
     * Retrieves all Product aggregates.
     *
     * @return List of all products
     */
    List<Product> findAll();
}
