package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.Product;

import java.math.BigDecimal;

/**
 * Inbound port driving the creation of new products.
 */
@FunctionalInterface
public interface CreateProductUseCase {

    /**
     * Creates a new product catalog aggregate.
     *
     * @param sku Stock Keeping Unit
     * @param name Product name
     * @param description Detailed description
     * @param price Selling price
     * @param currency 3-letter ISO currency code
     * @return The created Product aggregate
     */
    Product createProduct(String sku, String name, String description, BigDecimal price, String currency);
}
