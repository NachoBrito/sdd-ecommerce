package es.nachobrito.sdd.ecommerce.domain.model;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Order aggregate root representing an e-commerce customer order.
 *
 * @param id Unique identifier
 * @param customerName Full name of the customer placing the order
 * @param customerEmail Email address of the customer
 * @param date Timestamp when the order was placed
 * @param status Lifecycle status
 * @param totalAmountInCents Total monetary amount in integer cents
 * @param currency ISO-4217 3-letter currency code
 * @param deliveryAddress Destination delivery address
 * @param invoiceAddress Billing and invoicing address
 * @param lines Collection of individual order items
 */
public record Order(
        UUID id,
        String customerName,
        String customerEmail,
        Instant date,
        OrderStatus status,
        Integer totalAmountInCents,
        String currency,
        DeliveryAddress deliveryAddress,
        InvoiceAddress invoiceAddress,
        List<OrderLine> lines
) {

    public static final int MIN_TOTAL_AMOUNT_IN_CENTS = 1;
    public static final int ISO_CURRENCY_LENGTH = 3;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Order {
        Objects.requireNonNull(id, "Order id must not be null");
        Objects.requireNonNull(customerName, "Order customerName must not be null");
        Objects.requireNonNull(customerEmail, "Order customerEmail must not be null");
        Objects.requireNonNull(date, "Order date must not be null");
        Objects.requireNonNull(status, "Order status must not be null");
        Objects.requireNonNull(totalAmountInCents, "Order totalAmountInCents must not be null");
        Objects.requireNonNull(currency, "Order currency must not be null");
        Objects.requireNonNull(deliveryAddress, "Order deliveryAddress must not be null");
        Objects.requireNonNull(invoiceAddress, "Order invoiceAddress must not be null");
        Objects.requireNonNull(lines, "Order lines must not be null");

        if (customerName.isBlank()) {
            throw new InvalidOrderException("Customer name must not be blank");
        }
        if (customerEmail.isBlank() || !EMAIL_PATTERN.matcher(customerEmail).matches()) {
            throw new InvalidOrderException("Customer email must be a valid email address");
        }
        if (currency.length() != ISO_CURRENCY_LENGTH || !currency.matches("^[A-Z]{3}$")) {
            throw new InvalidOrderException("Currency must be a valid 3-letter ISO code");
        }
        if (lines.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one order line");
        }
        if (totalAmountInCents < MIN_TOTAL_AMOUNT_IN_CENTS) {
            throw new InvalidOrderException("Order total amount in cents must be at least 1 cent");
        }

        final int calculatedTotal = lines.stream()
                .mapToInt(OrderLine::lineTotalInCents)
                .sum();
        if (!totalAmountInCents.equals(calculatedTotal)) {
            throw new InvalidOrderException(
                    "Order total amount in cents (" + totalAmountInCents
                            + ") does not match sum of line totals (" + calculatedTotal + ")"
            );
        }

        lines = Collections.unmodifiableList(lines);
    }

    /**
     * Factory method creating a new Order in CREATED status with current UTC timestamp.
     *
     * @param id Client-provided unique order identifier
     * @param customerName Customer full name
     * @param customerEmail Customer email address
     * @param deliveryAddress Physical destination address
     * @param invoiceAddress Invoicing details
     * @param lines Collection of order lines
     * @param totalAmountInCents Total monetary amount in cents
     * @param currency Currency code
     * @return New Order aggregate
     */
    public static Order create(
            final UUID id,
            final String customerName,
            final String customerEmail,
            final DeliveryAddress deliveryAddress,
            final InvoiceAddress invoiceAddress,
            final List<OrderLine> lines,
            final Integer totalAmountInCents,
            final String currency
    ) {
        return new Order(
                id,
                customerName,
                customerEmail,
                Instant.now(),
                OrderStatus.CREATED,
                totalAmountInCents,
                currency,
                deliveryAddress,
                invoiceAddress,
                lines
        );
    }

    /**
     * Creates a new Order instance with an updated status.
     *
     * @param newStatus The new lifecycle status
     * @return New Order instance with updated status
     */
    public Order updateStatus(final OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Cannot update status of a cancelled order");
        }
        return new Order(
                this.id,
                this.customerName,
                this.customerEmail,
                this.date,
                newStatus,
                this.totalAmountInCents,
                this.currency,
                this.deliveryAddress,
                this.invoiceAddress,
                this.lines
        );
    }

    /**
     * Creates a new Order instance with CANCELLED status.
     *
     * @return New Order instance with CANCELLED status
     */
    public Order cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Order is already cancelled");
        }
        if (this.status == OrderStatus.DELIVERED) {
            throw new InvalidOrderException("Cannot cancel an already delivered order");
        }
        return new Order(
                this.id,
                this.customerName,
                this.customerEmail,
                this.date,
                OrderStatus.CANCELLED,
                this.totalAmountInCents,
                this.currency,
                this.deliveryAddress,
                this.invoiceAddress,
                this.lines
        );
    }
}
