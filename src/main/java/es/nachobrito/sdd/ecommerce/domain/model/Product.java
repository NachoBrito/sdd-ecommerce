package es.nachobrito.sdd.ecommerce.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Product aggregate root representing a sellable catalog item.
 *
 * @param id Unique identifier
 * @param sku Stock Keeping Unit
 * @param name Human-readable product name
 * @param description Detailed description
 * @param priceInCents Product price in integer cents
 * @param currency ISO-4217 currency code
 * @param status Lifecycle status
 * @param createdAt Creation timestamp
 * @param updatedAt Last update timestamp
 */
public record Product(
        UUID id,
        String sku,
        String name,
        String description,
        Integer priceInCents,
        String currency,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static final int MIN_PRICE_IN_CENTS = 1;

    public Product {
        Objects.requireNonNull(id, "Product id must not be null");
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("Product SKU must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (priceInCents == null || priceInCents < MIN_PRICE_IN_CENTS) {
            throw new IllegalArgumentException("Product price in cents must be at least " + MIN_PRICE_IN_CENTS);
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-letter ISO code");
        }
        Objects.requireNonNull(status, "Product status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Factory method to create a new Product in DRAFT status with client-supplied UUID.
     *
     * @param id Client-generated unique identifier
     * @param sku Stock keeping unit
     * @param name Name of the product
     * @param description Detailed description
     * @param priceInCents Price in integer cents
     * @param currency 3-letter ISO currency code
     * @return New Product aggregate
     */
    public static Product create(
            final UUID id,
            final String sku,
            final String name,
            final String description,
            final Integer priceInCents,
            final String currency
    ) {
        final Instant now = Instant.now();
        return new Product(
                id,
                sku,
                name,
                description,
                priceInCents,
                currency,
                ProductStatus.DRAFT,
                now,
                now
        );
    }

    /**
     * Transition product to ACTIVE status.
     *
     * @return Updated Product aggregate
     */
    public Product activate() {
        return new Product(
                this.id,
                this.sku,
                this.name,
                this.description,
                this.priceInCents,
                this.currency,
                ProductStatus.ACTIVE,
                this.createdAt,
                Instant.now()
        );
    }

    /**
     * Transition product to ARCHIVED status.
     *
     * @return Updated Product aggregate
     */
    public Product archive() {
        return new Product(
                this.id,
                this.sku,
                this.name,
                this.description,
                this.priceInCents,
                this.currency,
                ProductStatus.ARCHIVED,
                this.createdAt,
                Instant.now()
        );
    }
}
