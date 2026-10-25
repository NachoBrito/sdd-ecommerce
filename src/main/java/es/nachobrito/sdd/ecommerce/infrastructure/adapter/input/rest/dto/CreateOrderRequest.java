package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating a new order.
 *
 * @param id Client-generated unique order identifier
 * @param customerName Full name of the customer placing the order
 * @param customerEmail Email address of the customer
 * @param deliveryAddress Destination delivery address details
 * @param invoiceAddress Invoicing entity details
 * @param lines Collection of ordered line items
 * @param totalAmountInCents Total monetary amount of the order in integer cents
 * @param currency ISO-4217 3-letter currency code
 */
@Serdeable
public record CreateOrderRequest(
        @NotNull(message = "Order ID is required")
        UUID id,

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be a valid email address")
        String customerEmail,

        @NotNull(message = "Delivery address is required")
        @Valid
        DeliveryAddressDto deliveryAddress,

        @NotNull(message = "Invoice address is required")
        @Valid
        InvoiceAddressDto invoiceAddress,

        @NotEmpty(message = "Order lines must not be empty")
        List<@Valid OrderLineDto> lines,

        @NotNull(message = "totalAmountInCents is required")
        @Min(value = 1, message = "totalAmountInCents must be at least 1 cent")
        Integer totalAmountInCents,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code")
        String currency
) {

    public CreateOrderRequest {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    @Override
    public List<OrderLineDto> lines() {
        return List.copyOf(lines);
    }

    /**
     * Converts the delivery address DTO to domain model.
     *
     * @return DeliveryAddress domain value object
     */
    public DeliveryAddress toDeliveryAddress() {
        return new DeliveryAddress(
                deliveryAddress.name(),
                deliveryAddress.telephone(),
                deliveryAddress.street(),
                deliveryAddress.complement(),
                deliveryAddress.city(),
                deliveryAddress.state(),
                deliveryAddress.zipCode(),
                deliveryAddress.country()
        );
    }

    /**
     * Converts the invoice address DTO to domain model.
     *
     * @return InvoiceAddress domain value object
     */
    public InvoiceAddress toInvoiceAddress() {
        return new InvoiceAddress(
                invoiceAddress.name(),
                invoiceAddress.vatNumber(),
                invoiceAddress.telephone(),
                invoiceAddress.street(),
                invoiceAddress.complement(),
                invoiceAddress.city(),
                invoiceAddress.state(),
                invoiceAddress.zipCode(),
                invoiceAddress.country()
        );
    }

    /**
     * Converts the order lines DTO list to domain models.
     *
     * @return List of OrderLine domain entities
     */
    public List<OrderLine> toOrderLines() {
        return lines.stream()
                .map(line -> new OrderLine(
                        line.productId(),
                        line.quantity(),
                        line.unitPriceInCents(),
                        line.lineTotalInCents()
                ))
                .toList();
    }
}
