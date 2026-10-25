package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import es.nachobrito.sdd.ecommerce.domain.model.Product;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response payload representing a product in the catalog.
 *
 * @param id Unique identifier
 * @param sku Stock keeping unit
 * @param name Product name
 * @param description Detailed description
 * @param price Selling price
 * @param currency Currency code
 * @param status Lifecycle status
 * @param createdAt Creation timestamp
 * @param updatedAt Last update timestamp
 */
@Serdeable
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        @Nullable String description,
        BigDecimal price,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps a domain Product aggregate to a ProductResponse DTO.
     *
     * @param product Domain product aggregate
     * @return ProductResponse DTO
     */
    public static ProductResponse fromDomain(final Product product) {
        return new ProductResponse(
                product.id(),
                product.sku(),
                product.name(),
                product.description(),
                product.price(),
                product.currency(),
                product.status().name(),
                product.createdAt(),
                product.updatedAt()
        );
    }
}
