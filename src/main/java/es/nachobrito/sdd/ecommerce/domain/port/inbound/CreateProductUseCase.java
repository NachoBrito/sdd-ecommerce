package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.Product;

import java.util.UUID;

/**
 * Inbound port for creating a new product aggregate in the catalog.
 */
@FunctionalInterface
public interface CreateProductUseCase {

    /**
     * Creates and persists a new product with client-generated identifier.
     *
     * @param id Client-provided unique identifier
     * @param sku Stock keeping unit
     * @param name Human-readable name
     * @param description Optional description
     * @param priceInCents Price expressed in whole integer cents
     * @param currency ISO-4217 3-letter currency code
     * @return Created Product aggregate
     */
    Product createProduct(
            UUID id,
            String sku,
            String name,
            String description,
            Integer priceInCents,
            String currency
    );
}
