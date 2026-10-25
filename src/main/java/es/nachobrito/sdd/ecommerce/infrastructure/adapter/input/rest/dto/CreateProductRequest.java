package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for creating a new product.
 *
 * @param id Client-generated unique identifier
 * @param sku Stock keeping unit matching ^[A-Z0-9-]{3,32}$
 * @param name Product name
 * @param description Optional product description
 * @param priceInCents Product price in integer cents with minimum value 1
 * @param currency ISO-4217 3-letter currency code
 */
@Serdeable
public record CreateProductRequest(
        @NotNull(message = "Product ID is required")
        UUID id,

        @NotBlank(message = "SKU is required")
        @Pattern(regexp = "^[A-Z0-9-]{3,32}$", message = "SKU must match pattern ^[A-Z0-9-]{3,32}$")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 128, message = "Product name must be between 1 and 128 characters")
        String name,

        @Nullable
        @Size(max = 1024, message = "Product description must not exceed 1024 characters")
        String description,

        @NotNull(message = "priceInCents is required")
        @Min(value = 1, message = "priceInCents must be at least 1 cent")
        Integer priceInCents,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency
) {
}
