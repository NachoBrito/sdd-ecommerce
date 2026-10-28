package es.nachobrito.sdd.ecommerce.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DisplayName("OrderNotification Domain Model Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class OrderNotificationTest {

    private final UUID orderId = UUID.randomUUID();
    private final NotificationAddress deliveryAddress = new NotificationAddress(
            "Alice", "+34600000000", "Street 1", "Apt 2", "Madrid", "Madrid", "28001", "Spain", null
    );
    private final NotificationAddress invoiceAddress = new NotificationAddress(
            "Alice Inc", "+34600000000", "Street 1", "Apt 2", "Madrid", "Madrid", "28001", "Spain", "ES12345678"
    );
    private final NotificationLineItem lineItem = new NotificationLineItem(
            UUID.randomUUID(), 2, 1000, 2000
    );

    @Test
    @DisplayName("Should successfully construct OrderNotification with valid fields")
    void shouldConstructValidOrderNotification() {
        final OrderNotification notification = new OrderNotification(
                orderId,
                "Alice Smith",
                "alice@example.com",
                Instant.now(),
                "CREATED",
                2000,
                "EUR",
                deliveryAddress,
                invoiceAddress,
                List.of(lineItem),
                "trace-123"
        );

        assertNotNull(notification);
        assertEquals(orderId, notification.orderId());
        assertEquals("Alice Smith", notification.customerName());
        assertEquals("alice@example.com", notification.customerEmail());
        assertEquals(1, notification.lines().size());
        assertEquals("trace-123", notification.traceId());
    }

    @Test
    @DisplayName("Should reject blank customer name")
    void shouldRejectBlankCustomerName() {
        assertThrows(InvalidOrderException.class, () -> new OrderNotification(
                orderId, "   ", "alice@example.com", Instant.now(), "CREATED",
                2000, "EUR", deliveryAddress, invoiceAddress, List.of(lineItem), null
        ));
    }

    @Test
    @DisplayName("Should reject invalid customer email")
    void shouldRejectInvalidCustomerEmail() {
        assertThrows(InvalidOrderException.class, () -> new OrderNotification(
                orderId, "Alice", "not-an-email", Instant.now(), "CREATED",
                2000, "EUR", deliveryAddress, invoiceAddress, List.of(lineItem), null
        ));
    }

    @Test
    @DisplayName("Should reject non-positive total amount")
    void shouldRejectNonPositiveTotalAmount() {
        assertThrows(InvalidOrderException.class, () -> new OrderNotification(
                orderId, "Alice", "alice@example.com", Instant.now(), "CREATED",
                0, "EUR", deliveryAddress, invoiceAddress, List.of(lineItem), null
        ));
    }

    @Test
    @DisplayName("Should accept notification with exact minimum total amount")
    void shouldAcceptExactMinimumTotalAmount() {
        final NotificationLineItem line = new NotificationLineItem(UUID.randomUUID(), 1, 1, 1);
        final OrderNotification notification = new OrderNotification(
                orderId, "Alice", "alice@example.com", Instant.now(), "CREATED",
                1, "EUR", deliveryAddress, invoiceAddress, List.of(line), null
        );
        assertEquals(1, notification.totalAmountInCents());
    }

    @Test
    @DisplayName("Should reject invalid currency code length")
    void shouldRejectInvalidCurrencyCodeLength() {
        assertThrows(InvalidOrderException.class, () -> new OrderNotification(
                orderId, "Alice", "alice@example.com", Instant.now(), "CREATED",
                2000, "EURO", deliveryAddress, invoiceAddress, List.of(lineItem), null
        ));
    }

    @Test
    @DisplayName("Should reject empty lines list")
    void shouldRejectEmptyLinesList() {
        assertThrows(InvalidOrderException.class, () -> new OrderNotification(
                orderId, "Alice", "alice@example.com", Instant.now(), "CREATED",
                2000, "EUR", deliveryAddress, invoiceAddress, List.of(), null
        ));
    }

    @Test
    @DisplayName("Should defensively copy lines list making it unmodifiable")
    void shouldMakeLinesListUnmodifiable() {
        final List<NotificationLineItem> mutableLines = new ArrayList<>();
        mutableLines.add(lineItem);

        final OrderNotification notification = new OrderNotification(
                orderId, "Alice", "alice@example.com", Instant.now(), "CREATED",
                2000, "EUR", deliveryAddress, invoiceAddress, mutableLines, null
        );

        assertThrows(UnsupportedOperationException.class, () -> notification.lines().add(lineItem));
    }
}
