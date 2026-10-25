package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for creating a new product.
 *
 * @param sku Stock keeping unit matching ^[A-Z0-9-]{3,32}$
 * @param name Product name
 * @param description Optional product description
 * @param price Product price with minimum value 0.01
 * @param currency ISO-4217 3-letter currency code
 */
@Serdeable
public record CreateProductRequest(
        @NotBlank(message = "SKU is required")
        @Pattern(regexp = "^[A-Z0-9-]{3,32}$", message = "SKU must match pattern ^[A-Z0-9-]{3,32}$")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 128, message = "Product name must be between 1 and 128 characters")
        String name,

        @Nullable
        @Size(max = 1024, message = "Product description must not exceed 1024 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        BigDecimal price,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency
) {
}
