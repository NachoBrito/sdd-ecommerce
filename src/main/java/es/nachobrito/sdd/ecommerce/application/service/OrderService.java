package es.nachobrito.sdd.ecommerce.application.service;

import es.nachobrito.sdd.ecommerce.application.aop.StructuredLog;
import es.nachobrito.sdd.ecommerce.domain.event.OrderCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.exception.OrderAlreadyExistsException;
import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.CreateOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.GetOrderUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.ListOrdersUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.EventPublisherPort;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.OrderRepositoryPort;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating Order use cases.
 */
@Singleton
@StructuredLog
public class OrderService implements CreateOrderUseCase, GetOrderUseCase, ListOrdersUseCase {

    private final OrderRepositoryPort orderRepository;
    private final EventPublisherPort eventPublisher;

    /**
     * Constructs the application service with explicit constructor injection.
     *
     * @param orderRepository Outbound persistence repository port
     * @param eventPublisher Outbound event publisher port
     */
    public OrderService(
            final OrderRepositoryPort orderRepository,
            final EventPublisherPort eventPublisher
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public Order createOrder(
            final UUID id,
            final String customerName,
            final String customerEmail,
            final DeliveryAddress deliveryAddress,
            final InvoiceAddress invoiceAddress,
            final List<OrderLine> lines,
            final Integer totalAmountInCents,
            final String currency
    ) {
        Objects.requireNonNull(id, "Order id must not be null");

        if (orderRepository.existsById(id)) {
            throw new OrderAlreadyExistsException(id);
        }

        final Order order = Order.create(
                id,
                customerName,
                customerEmail,
                deliveryAddress,
                invoiceAddress,
                lines,
                totalAmountInCents,
                currency
        );

        final Order saved = orderRepository.save(order);
        final OrderCreatedEvent event = OrderCreatedEvent.create(
                saved.id(),
                saved.totalAmountInCents(),
                saved.currency(),
                null
        );
        eventPublisher.publish(event);
        return saved;
    }

    @Override
    public Optional<Order> getOrder(final UUID id) {
        Objects.requireNonNull(id, "Order id must not be null");
        return orderRepository.findById(id);
    }

    @Override
    public List<Order> listOrders() {
        return orderRepository.findAll();
    }
}
