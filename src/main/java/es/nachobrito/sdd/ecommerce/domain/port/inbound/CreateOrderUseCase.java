package es.nachobrito.sdd.ecommerce.domain.port.inbound;

import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for creating a new order aggregate.
 */
@FunctionalInterface
public interface CreateOrderUseCase {

    /**
     * Creates and persists a new order.
     *
     * @param id Client-provided unique identifier
     * @param customerName Customer full name
     * @param customerEmail Customer email address
     * @param deliveryAddress Destination delivery address
     * @param invoiceAddress Billing and invoicing address
     * @param lines Collection of individual order items
     * @param totalAmountInCents Total monetary amount in cents
     * @param currency ISO-4217 3-letter currency code
     * @return Created Order aggregate
     */
    Order createOrder(
            UUID id,
            String customerName,
            String customerEmail,
            DeliveryAddress deliveryAddress,
            InvoiceAddress invoiceAddress,
            List<OrderLine> lines,
            Integer totalAmountInCents,
            String currency
    );
}
