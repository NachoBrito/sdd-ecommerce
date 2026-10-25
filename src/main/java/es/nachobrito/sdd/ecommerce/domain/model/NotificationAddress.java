package es.nachobrito.sdd.ecommerce.domain.model;

import java.util.Objects;

/**
 * Value object representing address details formatted for order notifications.
 *
 * @param name Recipient or entity name
 * @param telephone Contact telephone number
 * @param street Street name and house number
 * @param complement Optional suite, apartment, or secondary address details
 * @param city City or municipality
 * @param state State or province
 * @param zipCode Postal or ZIP code
 * @param country Country name or code
 * @param vatNumber Optional VAT number for invoicing address
 */
public record NotificationAddress(
        String name,
        String telephone,
        String street,
        String complement,
        String city,
        String state,
        String zipCode,
        String country,
        String vatNumber
) {

    public NotificationAddress {
        Objects.requireNonNull(name, "Notification address name must not be null");
        Objects.requireNonNull(telephone, "Notification address telephone must not be null");
        Objects.requireNonNull(street, "Notification address street must not be null");
        Objects.requireNonNull(city, "Notification address city must not be null");
        Objects.requireNonNull(state, "Notification address state must not be null");
        Objects.requireNonNull(zipCode, "Notification address zipCode must not be null");
        Objects.requireNonNull(country, "Notification address country must not be null");
    }
}
