package es.nachobrito.sdd.ecommerce.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an individual order line item in an order notification.
 *
 * @param productId Unique identifier of the purchased product
 * @param quantity Quantity of items purchased
 * @param unitPriceInCents Unit price in integer cents
 * @param lineTotalInCents Total line amount in integer cents
 */
public record NotificationLineItem(
        UUID productId,
        Integer quantity,
        Integer unitPriceInCents,
        Integer lineTotalInCents
) {

    public NotificationLineItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitPriceInCents, "unitPriceInCents must not be null");
        Objects.requireNonNull(lineTotalInCents, "lineTotalInCents must not be null");
    }
}
