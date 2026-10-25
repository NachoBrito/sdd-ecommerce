package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/**
 * Micronaut Data JDBC persistence entity for order_lines table.
 */
@Serdeable
@MappedEntity("order_lines")
@SuppressWarnings("PMD.DataClass")
public class OrderLineEntity {

    @Id
    @MappedProperty("id")
    private UUID id;

    @MappedProperty("order_id")
    private UUID orderId;

    @MappedProperty("product_id")
    private UUID productId;

    @MappedProperty("quantity")
    private Integer quantity;

    @MappedProperty("unit_price_in_cents")
    private Integer unitPriceInCents;

    @MappedProperty("line_total_in_cents")
    private Integer lineTotalInCents;

    /**
     * Default constructor for serialization frameworks.
     */
    public OrderLineEntity() {
    }

    /**
     * Full parameter constructor.
     */
    public OrderLineEntity(
            final UUID id,
            final UUID orderId,
            final UUID productId,
            final Integer quantity,
            final Integer unitPriceInCents,
            final Integer lineTotalInCents
    ) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceInCents = unitPriceInCents;
        this.lineTotalInCents = lineTotalInCents;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(final UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(final UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getUnitPriceInCents() {
        return unitPriceInCents;
    }

    public void setUnitPriceInCents(final Integer unitPriceInCents) {
        this.unitPriceInCents = unitPriceInCents;
    }

    public Integer getLineTotalInCents() {
        return lineTotalInCents;
    }

    public void setLineTotalInCents(final Integer lineTotalInCents) {
        this.lineTotalInCents = lineTotalInCents;
    }
}
