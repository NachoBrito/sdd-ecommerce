package es.nachobrito.sdd.ecommerce.domain.model;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;

import java.util.Objects;

/**
 * Value object representing the billing and invoicing entity details for an order.
 *
 * @param name Billing entity or individual name
 * @param vatNumber Tax or VAT identification number
 * @param telephone Contact telephone number
 * @param street Street name and house number
 * @param complement Optional suite, apartment, or secondary address details
 * @param city City or municipality
 * @param state State or province
 * @param zipCode Postal or ZIP code
 * @param country Country name or code
 */
public record InvoiceAddress(
        String name,
        String vatNumber,
        String telephone,
        String street,
        String complement,
        String city,
        String state,
        String zipCode,
        String country
) {

    public InvoiceAddress {
        Objects.requireNonNull(name, "Invoice address name must not be null");
        Objects.requireNonNull(vatNumber, "Invoice address vatNumber must not be null");
        Objects.requireNonNull(telephone, "Invoice address telephone must not be null");
        Objects.requireNonNull(street, "Invoice address street must not be null");
        Objects.requireNonNull(city, "Invoice address city must not be null");
        Objects.requireNonNull(state, "Invoice address state must not be null");
        Objects.requireNonNull(zipCode, "Invoice address zipCode must not be null");
        Objects.requireNonNull(country, "Invoice address country must not be null");

        if (name.isBlank() || name.length() > 128) {
            throw new InvalidOrderException("Invoice address name must be between 1 and 128 characters");
        }
        if (vatNumber.isBlank() || vatNumber.length() > 32) {
            throw new InvalidOrderException("Invoice address vatNumber must be between 1 and 32 characters");
        }
        if (telephone.isBlank() || telephone.length() > 32) {
            throw new InvalidOrderException("Invoice address telephone must be between 1 and 32 characters");
        }
        if (street.isBlank() || street.length() > 255) {
            throw new InvalidOrderException("Invoice address street must be between 1 and 255 characters");
        }
        if (complement != null && complement.length() > 255) {
            throw new InvalidOrderException("Invoice address complement must not exceed 255 characters");
        }
        if (city.isBlank() || city.length() > 100) {
            throw new InvalidOrderException("Invoice address city must be between 1 and 100 characters");
        }
        if (state.isBlank() || state.length() > 100) {
            throw new InvalidOrderException("Invoice address state must be between 1 and 100 characters");
        }
        if (zipCode.isBlank() || zipCode.length() > 20) {
            throw new InvalidOrderException("Invoice address zipCode must be between 1 and 20 characters");
        }
        if (country.isBlank() || country.length() > 100) {
            throw new InvalidOrderException("Invoice address country must be between 1 and 100 characters");
        }
    }
}
