package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO representing delivery address in REST payloads.
 *
 * @param name Recipient name
 * @param telephone Contact telephone number
 * @param street Street name and house number
 * @param complement Optional suite or apartment information
 * @param city City or municipality
 * @param state State or province
 * @param zipCode Postal or ZIP code
 * @param country Country name or code
 */
@Serdeable
public record DeliveryAddressDto(
        @NotBlank(message = "Delivery address name is required")
        @Size(max = 128, message = "Delivery address name must not exceed 128 characters")
        String name,

        @NotBlank(message = "Delivery address telephone is required")
        @Size(max = 32, message = "Delivery address telephone must not exceed 32 characters")
        String telephone,

        @NotBlank(message = "Delivery address street is required")
        @Size(max = 255, message = "Delivery address street must not exceed 255 characters")
        String street,

        @Nullable
        @Size(max = 255, message = "Delivery address complement must not exceed 255 characters")
        String complement,

        @NotBlank(message = "Delivery address city is required")
        @Size(max = 100, message = "Delivery address city must not exceed 100 characters")
        String city,

        @NotBlank(message = "Delivery address state is required")
        @Size(max = 100, message = "Delivery address state must not exceed 100 characters")
        String state,

        @NotBlank(message = "Delivery address zipCode is required")
        @Size(max = 20, message = "Delivery address zipCode must not exceed 20 characters")
        String zipCode,

        @NotBlank(message = "Delivery address country is required")
        @Size(max = 100, message = "Delivery address country must not exceed 100 characters")
        String country
) {
}
