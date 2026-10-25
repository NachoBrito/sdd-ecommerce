package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto;

import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response payload representing an order in REST responses.
 *
 * @param id Order unique identifier
 * @param customerName Customer full name
 * @param customerEmail Customer email address
 * @param date Timestamp when the order was placed
 * @param status Lifecycle status string
 * @param totalAmountInCents Total monetary amount in integer cents
 * @param currency ISO-4217 3-letter currency code
 * @param deliveryAddress Destination delivery address details
 * @param invoiceAddress Invoicing entity details
 * @param lines Collection of individual order items
 */
@Serdeable
public record OrderResponse(
        UUID id,
        String customerName,
        String customerEmail,
        Instant date,
        String status,
        Integer totalAmountInCents,
        String currency,
        DeliveryAddressDto deliveryAddress,
        InvoiceAddressDto invoiceAddress,
        List<OrderLineDto> lines
) {

    public OrderResponse {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    @Override
    public List<OrderLineDto> lines() {
        return List.copyOf(lines);
    }

    /**
     * Maps a domain Order aggregate to an OrderResponse DTO.
     *
     * @param order Domain order aggregate
     * @return OrderResponse DTO
     */
    public static OrderResponse fromDomain(final Order order) {
        final DeliveryAddress delivery = order.deliveryAddress();
        final DeliveryAddressDto deliveryDto = new DeliveryAddressDto(
                delivery.name(),
                delivery.telephone(),
                delivery.street(),
                delivery.complement(),
                delivery.city(),
                delivery.state(),
                delivery.zipCode(),
                delivery.country()
        );

        final InvoiceAddress invoice = order.invoiceAddress();
        final InvoiceAddressDto invoiceDto = new InvoiceAddressDto(
                invoice.name(),
                invoice.vatNumber(),
                invoice.telephone(),
                invoice.street(),
                invoice.complement(),
                invoice.city(),
                invoice.state(),
                invoice.zipCode(),
                invoice.country()
        );

        final List<OrderLineDto> lineDtos = order.lines().stream()
                .map(OrderResponse::toLineDto)
                .toList();

        return new OrderResponse(
                order.id(),
                order.customerName(),
                order.customerEmail(),
                order.date(),
                order.status().name(),
                order.totalAmountInCents(),
                order.currency(),
                deliveryDto,
                invoiceDto,
                lineDtos
        );
    }

    private static OrderLineDto toLineDto(final OrderLine line) {
        return new OrderLineDto(
                line.productId(),
                line.quantity(),
                line.unitPriceInCents(),
                line.lineTotalInCents()
        );
    }
}
