package es.nachobrito.sdd.ecommerce.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.nachobrito.sdd.ecommerce.domain.exception.InvalidOrderException;
import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pure unit tests for Order aggregate root and value object business invariants.
 */
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.TooManyMethods"})
class OrderTest {

    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String DEFAULT_CITY = "Madrid";
    private static final String DEFAULT_COUNTRY = "Spain";
    private static final String DEFAULT_CUSTOMER_NAME = "John Doe";
    private static final String DEFAULT_CUSTOMER_EMAIL = "john.doe@example.com";

    private DeliveryAddress createValidDeliveryAddress() {
        return new DeliveryAddress(
                "Jane Doe",
                "+34600112233",
                "Gran Via 12",
                "Apt 4B",
                DEFAULT_CITY,
                DEFAULT_CITY,
                "28013",
                DEFAULT_COUNTRY
        );
    }

    private InvoiceAddress createValidInvoiceAddress() {
        return new InvoiceAddress(
                "Acme Corp",
                "ESB12345678",
                "+34911223344",
                "Paseo de la Castellana 100",
                null,
                DEFAULT_CITY,
                DEFAULT_CITY,
                "28046",
                DEFAULT_COUNTRY
        );
    }

    private OrderLine createValidOrderLine(final int quantity, final int unitPriceInCents) {
        return new OrderLine(
                UUID.randomUUID(),
                quantity,
                unitPriceInCents,
                quantity * unitPriceInCents
        );
    }

    @Test
    @DisplayName("Should create order with valid parameters in CREATED status")
    void shouldCreateOrderInCreatedStatus() {
        final UUID id = UUID.randomUUID();
        final DeliveryAddress delivery = createValidDeliveryAddress();
        final InvoiceAddress invoice = createValidInvoiceAddress();
        final OrderLine line1 = createValidOrderLine(2, 1_500); // 3000
        final OrderLine line2 = createValidOrderLine(1, 2_000); // 2000
        final int totalAmount = 5_000;

        final Order order = Order.create(
                id,
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                delivery,
                invoice,
                List.of(line1, line2),
                totalAmount,
                DEFAULT_CURRENCY
        );

        assertAll("Order creation assertions",
                () -> assertEquals(id, order.id(), "Order ID must match client-supplied UUID"),
                () -> assertEquals(DEFAULT_CUSTOMER_NAME, order.customerName(), "Customer name must match"),
                () -> assertEquals(DEFAULT_CUSTOMER_EMAIL, order.customerEmail(), "Customer email must match"),
                () -> assertEquals(OrderStatus.CREATED, order.status(), "Status must be CREATED upon creation"),
                () -> assertEquals(totalAmount, order.totalAmountInCents(), "Total amount must match sum of lines"),
                () -> assertEquals(DEFAULT_CURRENCY, order.currency(), "Currency must match expected value"),
                () -> assertEquals(2, order.lines().size(), "Must contain 2 order lines"),
                () -> assertNotNull(order.date(), "Order date must not be null"),
                () -> assertEquals(delivery, order.deliveryAddress(), "Delivery address must match"),
                () -> assertEquals(invoice, order.invoiceAddress(), "Invoice address must match")
        );
    }

    @Test
    @DisplayName("Should reject null order ID")
    void shouldRejectNullOrderId() {
        assertThrows(NullPointerException.class, () -> Order.create(
                null,
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                createValidDeliveryAddress(),
                createValidInvoiceAddress(),
                List.of(createValidOrderLine(1, 1000)),
                1000,
                DEFAULT_CURRENCY
        ), "Creating order with null ID must fail");
    }

    @Test
    @DisplayName("Should reject order with blank customer name")
    void shouldRejectBlankCustomerName() {
        final UUID id = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> Order.create(
                id,
                "   ",
                DEFAULT_CUSTOMER_EMAIL,
                createValidDeliveryAddress(),
                createValidInvoiceAddress(),
                List.of(createValidOrderLine(1, 1000)),
                1000,
                DEFAULT_CURRENCY
        ), "Creating order with blank customer name must fail");
    }

    @Test
    @DisplayName("Should reject order with invalid customer email")
    void shouldRejectInvalidCustomerEmail() {
        final UUID id = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> Order.create(
                id,
                DEFAULT_CUSTOMER_NAME,
                "invalid-email-format",
                createValidDeliveryAddress(),
                createValidInvoiceAddress(),
                List.of(createValidOrderLine(1, 1000)),
                1000,
                DEFAULT_CURRENCY
        ), "Creating order with invalid customer email must fail");
    }

    @Test
    @DisplayName("Should reject empty order lines list")
    void shouldRejectEmptyLines() {
        final UUID id = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> Order.create(
                id,
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                createValidDeliveryAddress(),
                createValidInvoiceAddress(),
                Collections.emptyList(),
                1000,
                DEFAULT_CURRENCY
        ), "Creating order with empty lines must fail");
    }

    @Test
    @DisplayName("Should reject order total mismatch with sum of line totals")
    void shouldRejectTotalAmountMismatch() {
        final UUID id = UUID.randomUUID();
        final OrderLine line = createValidOrderLine(2, 1000); // line total = 2000
        assertThrows(InvalidOrderException.class, () -> Order.create(
                id,
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                createValidDeliveryAddress(),
                createValidInvoiceAddress(),
                List.of(line),
                2500, // mismatch
                DEFAULT_CURRENCY
        ), "Creating order with mismatched total amount must fail");
    }

    @Test
    @DisplayName("Should reject order line with quantity less than 1")
    void shouldRejectInvalidLineQuantity() {
        final UUID productId = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> new OrderLine(
                productId,
                0,
                1000,
                0
        ), "Order line with quantity < 1 must fail");
    }

    @Test
    @DisplayName("Should reject order line with unit price less than 1 cent")
    void shouldRejectInvalidLineUnitPrice() {
        final UUID productId = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> new OrderLine(
                productId,
                1,
                0,
                0
        ), "Order line with unit price < 1 must fail");
    }

    @Test
    @DisplayName("Should reject order line where lineTotalInCents does not equal quantity * unitPrice")
    void shouldRejectInvalidLineTotal() {
        final UUID productId = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> new OrderLine(
                productId,
                2,
                1000,
                1500 // should be 2000
        ), "Order line with incorrect line total must fail");
    }

    @Test
    @DisplayName("Should reject invalid currency format")
    void shouldRejectInvalidCurrency() {
        final UUID id = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> Order.create(
                id,
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                createValidDeliveryAddress(),
                createValidInvoiceAddress(),
                List.of(createValidOrderLine(1, 1000)),
                1000,
                "US" // invalid length
        ), "Currency with invalid length must fail");

        assertThrows(InvalidOrderException.class, () -> Order.create(
                id,
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                createValidDeliveryAddress(),
                createValidInvoiceAddress(),
                List.of(createValidOrderLine(1, 1000)),
                1000,
                "usd" // lowercase
        ), "Currency with lowercase characters must fail");
    }

    @Test
    @DisplayName("Should reject delivery address with blank mandatory fields")
    void shouldRejectBlankDeliveryAddressFields() {
        assertThrows(InvalidOrderException.class, () -> new DeliveryAddress(
                "",
                "+34600112233",
                "Street 1",
                null,
                "City",
                "State",
                "28001",
                DEFAULT_COUNTRY
        ), "Delivery address with blank name must fail");
    }

    @Test
    @DisplayName("Should reject invoice address with blank VAT number")
    void shouldRejectBlankInvoiceVatNumber() {
        assertThrows(InvalidOrderException.class, () -> new InvoiceAddress(
                "Company",
                "",
                "+34600112233",
                "Street 1",
                null,
                "City",
                "State",
                "28001",
                DEFAULT_COUNTRY
        ), "Invoice address with blank VAT number must fail");
    }
}
