package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

/**
 * Micronaut Data JDBC persistence entity for products table.
 */
@Serdeable
@MappedEntity("products")
@SuppressWarnings("PMD.DataClass")
public class ProductEntity {

    @Id
    @MappedProperty("id")
    private UUID id;

    @MappedProperty("sku")
    private String sku;

    @MappedProperty("name")
    private String name;

    @Nullable
    @MappedProperty("description")
    private String description;

    @MappedProperty("price_in_cents")
    private Integer priceInCents;

    @MappedProperty("currency")
    private String currency;

    @MappedProperty("status")
    private String status;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    @DateUpdated
    @MappedProperty("updated_at")
    private Instant updatedAt;

    /**
     * Default constructor for serialization frameworks.
     */
    public ProductEntity() {
    }

    /**
     * Full parameter constructor.
     *
     * @param id Unique identifier
     * @param sku Stock keeping unit
     * @param name Product name
     * @param description Detailed description
     * @param priceInCents Selling price in integer cents
     * @param currency Currency code
     * @param status Lifecycle status
     * @param createdAt Creation timestamp
     * @param updatedAt Last update timestamp
     */
    public ProductEntity(
            final UUID id,
            final String sku,
            final String name,
            final String description,
            final Integer priceInCents,
            final String currency,
            final String status,
            final Instant createdAt,
            final Instant updatedAt
    ) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.priceInCents = priceInCents;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(final String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getPriceInCents() {
        return priceInCents;
    }

    public void setPriceInCents(final Integer priceInCents) {
        this.priceInCents = priceInCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
