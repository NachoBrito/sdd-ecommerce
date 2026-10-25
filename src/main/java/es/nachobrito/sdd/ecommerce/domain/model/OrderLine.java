package es.nachobrito.sdd.ecommerce.domain.model;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity within an Order aggregate representing an individual line item.
 *
 * @param productId Identifier of the purchased product
 * @param quantity Quantity of items ordered
 * @param unitPriceInCents Price per single unit in integer cents
 * @param lineTotalInCents Total monetary amount for this line item in integer cents
 */
public record OrderLine(
        UUID productId,
        Integer quantity,
        Integer unitPriceInCents,
        Integer lineTotalInCents
) {

    public static final int MIN_QUANTITY = 1;
    public static final int MIN_UNIT_PRICE_IN_CENTS = 1;

    public OrderLine {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitPriceInCents, "unitPriceInCents must not be null");
        Objects.requireNonNull(lineTotalInCents, "lineTotalInCents must not be null");

        if (quantity < MIN_QUANTITY) {
            throw new InvalidOrderException("Order line quantity must be at least 1");
        }
        if (unitPriceInCents < MIN_UNIT_PRICE_IN_CENTS) {
            throw new InvalidOrderException("Order line unitPriceInCents must be at least 1 cent");
        }
        final int expectedLineTotal = quantity * unitPriceInCents;
        if (!lineTotalInCents.equals(expectedLineTotal)) {
            throw new InvalidOrderException(
                    "Order line total (" + lineTotalInCents + ") does not match quantity * unit price ("
                            + expectedLineTotal + ")"
            );
        }
    }
}
