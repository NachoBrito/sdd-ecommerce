package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import es.nachobrito.sdd.ecommerce.domain.model.DeliveryAddress;
import es.nachobrito.sdd.ecommerce.domain.model.InvoiceAddress;
import es.nachobrito.sdd.ecommerce.domain.model.Order;
import es.nachobrito.sdd.ecommerce.domain.model.OrderLine;
import es.nachobrito.sdd.ecommerce.domain.model.OrderStatus;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.OrderRepositoryPort;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing OrderRepositoryPort using Micronaut Data JDBC.
 */
@Singleton
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final MicronautOrderRepository orderRepository;
    private final MicronautOrderLineRepository orderLineRepository;

    /**
     * Constructs the persistence adapter with explicit constructor injection.
     *
     * @param orderRepository Compile-time Micronaut Data JDBC repository for orders
     * @param orderLineRepository Compile-time Micronaut Data JDBC repository for order lines
     */
    public OrderPersistenceAdapter(
            final MicronautOrderRepository orderRepository,
            final MicronautOrderLineRepository orderLineRepository
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.orderLineRepository = Objects.requireNonNull(orderLineRepository, "orderLineRepository must not be null");
    }

    @Override
    public Order save(final Order order) {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(order.id(), "order id must not be null");

        final OrderEntity entity = toEntity(order);
        final OrderEntity savedOrder = orderRepository.existsById(order.id())
                ? orderRepository.update(entity)
                : orderRepository.save(entity);

        orderLineRepository.deleteByOrderId(order.id());

        final List<OrderLine> savedLines = new ArrayList<>();
        for (final OrderLine line : order.lines()) {
            final OrderLineEntity lineEntity = toLineEntity(order.id(), line);
            final OrderLineEntity savedLine = orderLineRepository.save(lineEntity);
            savedLines.add(toLineDomain(savedLine));
        }

        return toDomain(savedOrder, savedLines);
    }

    @Override
    public Optional<Order> findById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return orderRepository.findById(id).map(orderEntity -> {
            final List<OrderLineEntity> lineEntities = orderLineRepository.findByOrderId(id);
            final List<OrderLine> lines = lineEntities.stream()
                    .map(this::toLineDomain)
                    .toList();
            return toDomain(orderEntity, lines);
        });
    }

    @Override
    public boolean existsById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return orderRepository.existsById(id);
    }

    @Override
    public List<Order> findAll() {
        final List<Order> orders = new ArrayList<>();
        for (final OrderEntity orderEntity : orderRepository.findAll()) {
            final List<OrderLineEntity> lineEntities = orderLineRepository.findByOrderId(orderEntity.getId());
            final List<OrderLine> lines = lineEntities.stream()
                    .map(this::toLineDomain)
                    .toList();
            orders.add(toDomain(orderEntity, lines));
        }
        return List.copyOf(orders);
    }

    private OrderEntity toEntity(final Order domain) {
        final Instant now = Instant.now();
        return new OrderEntity(
                domain.id(),
                domain.customerName(),
                domain.customerEmail(),
                domain.date(),
                domain.status().name(),
                domain.totalAmountInCents(),
                domain.currency(),
                domain.deliveryAddress().name(),
                domain.deliveryAddress().telephone(),
                domain.deliveryAddress().street(),
                domain.deliveryAddress().complement(),
                domain.deliveryAddress().city(),
                domain.deliveryAddress().state(),
                domain.deliveryAddress().zipCode(),
                domain.deliveryAddress().country(),
                domain.invoiceAddress().name(),
                domain.invoiceAddress().vatNumber(),
                domain.invoiceAddress().telephone(),
                domain.invoiceAddress().street(),
                domain.invoiceAddress().complement(),
                domain.invoiceAddress().city(),
                domain.invoiceAddress().state(),
                domain.invoiceAddress().zipCode(),
                domain.invoiceAddress().country(),
                now,
                now
        );
    }

    private OrderLineEntity toLineEntity(final UUID orderId, final OrderLine line) {
        return new OrderLineEntity(
                UUID.randomUUID(),
                orderId,
                line.productId(),
                line.quantity(),
                line.unitPriceInCents(),
                line.lineTotalInCents()
        );
    }

    private Order toDomain(final OrderEntity entity, final List<OrderLine> lines) {
        final DeliveryAddress deliveryAddress = new DeliveryAddress(
                entity.getDeliveryName(),
                entity.getDeliveryTelephone(),
                entity.getDeliveryStreet(),
                entity.getDeliveryComplement(),
                entity.getDeliveryCity(),
                entity.getDeliveryState(),
                entity.getDeliveryZipCode(),
                entity.getDeliveryCountry()
        );

        final InvoiceAddress invoiceAddress = new InvoiceAddress(
                entity.getInvoiceName(),
                entity.getInvoiceVatNumber(),
                entity.getInvoiceTelephone(),
                entity.getInvoiceStreet(),
                entity.getInvoiceComplement(),
                entity.getInvoiceCity(),
                entity.getInvoiceState(),
                entity.getInvoiceZipCode(),
                entity.getInvoiceCountry()
        );

        return new Order(
                entity.getId(),
                entity.getCustomerName(),
                entity.getCustomerEmail(),
                entity.getDate(),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getTotalAmountInCents(),
                entity.getCurrency(),
                deliveryAddress,
                invoiceAddress,
                lines
        );
    }

    private OrderLine toLineDomain(final OrderLineEntity entity) {
        return new OrderLine(
                entity.getProductId(),
                entity.getQuantity(),
                entity.getUnitPriceInCents(),
                entity.getLineTotalInCents()
        );
    }
}
