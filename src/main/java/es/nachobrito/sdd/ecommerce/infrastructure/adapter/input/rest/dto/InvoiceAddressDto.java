package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO representing invoice address in REST payloads.
 *
 * @param name Billing entity name
 * @param vatNumber Tax or VAT identification number
 * @param telephone Contact telephone number
 * @param street Street name and house number
 * @param complement Optional suite or apartment information
 * @param city City or municipality
 * @param state State or province
 * @param zipCode Postal or ZIP code
 * @param country Country name or code
 */
@Serdeable
public record InvoiceAddressDto(
        @NotBlank(message = "Invoice address name is required")
        @Size(max = 128, message = "Invoice address name must not exceed 128 characters")
        String name,

        @NotBlank(message = "Invoice address vatNumber is required")
        @Size(max = 32, message = "Invoice address vatNumber must not exceed 32 characters")
        String vatNumber,

        @NotBlank(message = "Invoice address telephone is required")
        @Size(max = 32, message = "Invoice address telephone must not exceed 32 characters")
        String telephone,

        @NotBlank(message = "Invoice address street is required")
        @Size(max = 255, message = "Invoice address street must not exceed 255 characters")
        String street,

        @Nullable
        @Size(max = 255, message = "Invoice address complement must not exceed 255 characters")
        String complement,

        @NotBlank(message = "Invoice address city is required")
        @Size(max = 100, message = "Invoice address city must not exceed 100 characters")
        String city,

        @NotBlank(message = "Invoice address state is required")
        @Size(max = 100, message = "Invoice address state must not exceed 100 characters")
        String state,

        @NotBlank(message = "Invoice address zipCode is required")
        @Size(max = 20, message = "Invoice address zipCode must not exceed 20 characters")
        String zipCode,

        @NotBlank(message = "Invoice address country is required")
        @Size(max = 100, message = "Invoice address country must not exceed 100 characters")
        String country
) {
}
