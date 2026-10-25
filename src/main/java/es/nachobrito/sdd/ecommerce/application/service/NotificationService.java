package es.nachobrito.sdd.ecommerce.application.service;

import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.NotificationAddress;
import es.nachobrito.sdd.ecommerce.domain.model.NotificationLineItem;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import es.nachobrito.sdd.ecommerce.domain.model.OrderNotification;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.GetOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.SendOrderNotificationUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.NotificationTransportPort;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating customer order notifications across multiple registered transports.
 */
@Singleton
public class NotificationService implements SendOrderNotificationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final GetOrderUseCase getOrderUseCase;
    private final List<NotificationTransportPort> transports;

    public NotificationService(
            final GetOrderUseCase getOrderUseCase,
            final List<NotificationTransportPort> transports
    ) {
        this.getOrderUseCase = Objects.requireNonNull(getOrderUseCase, "getOrderUseCase must not be null");
        this.transports = transports != null ? Collections.unmodifiableList(transports) : List.of();
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void sendOrderNotification(final UUID orderId, final String traceId) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        LOG.info("Processing order notification trigger for orderId={}, traceId={}", orderId, traceId);

        final Optional<Order> orderOptional = getOrderUseCase.getOrder(orderId);
        if (orderOptional.isEmpty()) {
            LOG.warn("Order not found for notification dispatch: orderId={}", orderId);
            return;
        }

        final Order order = orderOptional.get();
        final OrderNotification notification = mapToNotification(order, traceId);

        final List<NotificationTransportPort> activeTransports = transports.stream()
                .filter(NotificationTransportPort::isEnabled)
                .toList();

        if (activeTransports.isEmpty()) {
            LOG.warn("Zero active notification transports configured. Notification skipped for orderId={}", orderId);
            return;
        }

        for (final NotificationTransportPort transport : activeTransports) {
            try {
                LOG.debug("Routing notification for orderId={} to transport={}", orderId, transport.transportName());
                transport.send(notification);
            } catch (Exception e) {
                LOG.error("Unhandled error dispatching to transport {} for orderId={}: {}",
                        transport.transportName(), orderId, e.getMessage(), e);
            }
        }

        LOG.info("Dispatched order notification to {} transport(s) for orderId={}",
                activeTransports.size(), orderId);
    }

    private OrderNotification mapToNotification(final Order order, final String traceId) {
        final NotificationAddress delivery = mapDeliveryAddress(order.deliveryAddress());
        final NotificationAddress invoice = mapInvoiceAddress(order.invoiceAddress());
        final List<NotificationLineItem> lines = mapLineItems(order.lines());

        return new OrderNotification(
                order.id(),
                order.customerName(),
                order.customerEmail(),
                order.date(),
                order.status().name(),
                order.totalAmountInCents(),
                order.currency(),
                delivery,
                invoice,
                lines,
                traceId
        );
    }

    private NotificationAddress mapDeliveryAddress(final DeliveryAddress address) {
        return new NotificationAddress(
                address.name(),
                address.telephone(),
                address.street(),
                address.complement(),
                address.city(),
                address.state(),
                address.zipCode(),
                address.country(),
                null
        );
    }

    private NotificationAddress mapInvoiceAddress(final InvoiceAddress address) {
        return new NotificationAddress(
                address.name(),
                address.telephone(),
                address.street(),
                address.complement(),
                address.city(),
                address.state(),
                address.zipCode(),
                address.country(),
                address.vatNumber()
        );
    }

    private List<NotificationLineItem> mapLineItems(final List<OrderLine> lines) {
        final List<NotificationLineItem> items = new ArrayList<>(lines.size());
        for (final OrderLine line : lines) {
            items.add(new NotificationLineItem(
                    line.productId(),
                    line.quantity(),
                    line.unitPriceInCents(),
                    line.lineTotalInCents()
            ));
        }
        return items;
    }
}
