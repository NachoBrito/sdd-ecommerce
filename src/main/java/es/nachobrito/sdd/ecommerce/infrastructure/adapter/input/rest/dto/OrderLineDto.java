package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO representing an order line item in REST payloads.
 *
 * @param productId Identifier of the purchased product
 * @param quantity Number of units ordered
 * @param unitPriceInCents Unit price in integer cents
 * @param lineTotalInCents Total line amount in integer cents
 */
@Serdeable
public record OrderLineDto(
        @NotNull(message = "Order line productId is required")
        UUID productId,

        @NotNull(message = "Order line quantity is required")
        @Min(value = 1, message = "Order line quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Order line unitPriceInCents is required")
        @Min(value = 1, message = "Order line unitPriceInCents must be at least 1 cent")
        Integer unitPriceInCents,

        @NotNull(message = "Order line lineTotalInCents is required")
        @Min(value = 1, message = "Order line lineTotalInCents must be at least 1 cent")
        Integer lineTotalInCents
) {
}
