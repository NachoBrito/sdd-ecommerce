package es.nachobrito.sdd.ecommerce.domain.model;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Domain model representing complete order details prepared for notification dispatch.
 *
 * @param orderId Unique identifier of the placed order
 * @param customerName Full name of the customer
 * @param customerEmail Email address of the customer recipient
 * @param orderDate Instant when the order was placed
 * @param status Current lifecycle status of the order
 * @param totalAmountInCents Grand total monetary amount in integer cents
 * @param currency ISO-4217 three-letter currency code
 * @param deliveryAddress Destination delivery address details
 * @param invoiceAddress Billing and invoicing address details
 * @param lines Collection of individual purchased line items
 * @param traceId Optional distributed tracing correlation identifier
 */
public record OrderNotification(
        UUID orderId,
        String customerName,
        String customerEmail,
        Instant orderDate,
        String status,
        Integer totalAmountInCents,
        String currency,
        NotificationAddress deliveryAddress,
        NotificationAddress invoiceAddress,
        List<NotificationLineItem> lines,
        String traceId
) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static final int ISO_CURRENCY_LENGTH = 3;
    public static final int MIN_TOTAL_AMOUNT = 1;

    public OrderNotification {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(customerName, "customerName must not be null");
        Objects.requireNonNull(customerEmail, "customerEmail must not be null");
        Objects.requireNonNull(orderDate, "orderDate must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(totalAmountInCents, "totalAmountInCents must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(deliveryAddress, "deliveryAddress must not be null");
        Objects.requireNonNull(invoiceAddress, "invoiceAddress must not be null");
        Objects.requireNonNull(lines, "lines must not be null");

        if (customerName.isBlank()) {
            throw new InvalidOrderException("Customer name must not be blank");
        }
        if (customerEmail.isBlank() || !EMAIL_PATTERN.matcher(customerEmail).matches()) {
            throw new InvalidOrderException("Customer email must be a valid email address");
        }
        if (status.isBlank()) {
            throw new InvalidOrderException("Status must not be blank");
        }
        if (totalAmountInCents < MIN_TOTAL_AMOUNT) {
            throw new InvalidOrderException("Total amount in cents must be at least 1");
        }
        if (currency.length() != ISO_CURRENCY_LENGTH) {
            throw new InvalidOrderException("Currency must be an ISO-4217 3-letter code");
        }
        if (lines.isEmpty()) {
            throw new InvalidOrderException("Order notification must contain at least one line item");
        }

        lines = List.copyOf(lines);
    }
}
