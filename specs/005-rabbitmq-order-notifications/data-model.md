# Data Model: Asynchronous Order Notifications via RabbitMQ

**Feature**: `005-rabbitmq-order-notifications`  
**Date**: 2026-10-27  

## Domain Models & Events

### 1. Domain Events

#### `OrderCreatedEvent` (Existing)
Emitted when a new Order aggregate is placed and committed.
- `eventId`: UUID - Unique event ID
- `eventType`: String - `"order.created.v1"`
- `orderId`: UUID - Identifier of the created order
- `totalAmountInCents`: Integer - Total amount in cents
- `currency`: String - 3-letter ISO code
- `timestamp`: Instant - Event creation timestamp in UTC
- `traceId`: String - Correlation trace ID

#### `OrderUpdatedEvent` (New)
Emitted when an Order aggregate status or details are updated.
- `eventId`: UUID - Unique event ID
- `eventType`: String - `"order.updated.v1"`
- `orderId`: UUID - Identifier of the updated order
- `status`: OrderStatus - New status (`CONFIRMED`, `SHIPPED`, `DELIVERED`, etc.)
- `timestamp`: Instant - Event timestamp in UTC
- `traceId`: String - Correlation trace ID

#### `OrderCancelledEvent` (New)
Emitted when an Order aggregate is cancelled.
- `eventId`: UUID - Unique event ID
- `eventType`: String - `"order.cancelled.v1"`
- `orderId`: UUID - Identifier of the cancelled order
- `reason`: String - Optional cancellation reason
- `timestamp`: Instant - Event timestamp in UTC
- `traceId`: String - Correlation trace ID

---

## Domain Ports

### Driving (Inbound) Ports

#### `UpdateOrderStatusUseCase`
```java
public interface UpdateOrderStatusUseCase {
    Order updateOrderStatus(UUID orderId, OrderStatus newStatus, String traceId);
}
```

#### `CancelOrderUseCase`
```java
public interface CancelOrderUseCase {
    Order cancelOrder(UUID orderId, String reason, String traceId);
}
```

### Driven (Outbound) Ports

#### `EventPublisherPort` (Updated)
```java
public interface EventPublisherPort {
    void publish(ProductCreatedEvent event);
    void publish(OrderCreatedEvent event);
    void publish(OrderUpdatedEvent event);
    void publish(OrderCancelledEvent event);
}
```

---

## Infrastructure Wire Contracts (Boundary DTO)

### `OrderEventMessage`
Serdeable record representing the wire payload published to RabbitMQ.
```java
@Serdeable
public record OrderEventMessage(
        UUID eventId,
        String eventType,
        UUID orderId,
        String orderStatus,
        Integer totalAmountInCents,
        String currency,
        Instant timestamp,
        String traceId
) {}
```

---

## RabbitMQ Topology

- **Exchange**: `ecommerce.orders.exchange` (Type: `topic`, Durable: `true`)
- **Queue**: `ecommerce.orders.notifications.queue` (Durable: `true`)
- **Routing Keys**:
  - `order.created` -> Published on order creation
  - `order.updated` -> Published on order update
  - `order.cancelled` -> Published on order cancellation
- **Binding**: Queue bound to exchange with pattern `order.*`
