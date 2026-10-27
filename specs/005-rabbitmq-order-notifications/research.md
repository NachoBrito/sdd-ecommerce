# Research & Architecture Decisions: Asynchronous Order Notifications via RabbitMQ

**Feature**: `005-rabbitmq-order-notifications`  
**Date**: 2026-10-27  

## Architectural Decisions

### Decision 1: Justification for RabbitMQ Broker (Constitution Principle 1 Exemption)

- **Decision**: Introduce RabbitMQ message broker for asynchronous order notification dispatch while keeping producer and consumer in the same deployable service unit.
- **Rationale**:
  - Satisfies Constitution Principle 1, Condition 2 (**Genuinely independent compute lifecycle & failure isolation**): Sending notifications across external network transports (SMTP mail servers, future SMS/push APIs) exhibits high latency variance and unpredictable external failure modes. Decoupling notification dispatch via RabbitMQ ensures that customer-facing order transactions commit with sub-50ms latency without blocking on external transport delivery.
  - Satisfies user specification: "The consumer is part of the same notification service." No microservice fragmentation is introduced; the single deployable unit pattern is preserved.
- **Alternatives considered**:
  - *Internal in-memory Micronaut Event Bus*: Already in place, but does not provide durable queueing, backpressure, or fault tolerance if the application crashes during peak bursts.
  - *Database-backed Outbox Table*: More complex schema management, requires polling threads, higher database write contention. RabbitMQ natively integrates with Micronaut and TestResources.

---

### Decision 2: RabbitMQ Exchange, Queue, and Routing Strategy

- **Decision**: Configure a Topic Exchange `ecommerce.orders.exchange` with topic routing keys `order.created`, `order.updated`, and `order.cancelled`. Bind the queue `ecommerce.orders.notifications.queue` using the pattern `order.*`.
- **Rationale**:
  - Topic exchanges provide future extensibility for other consumers (e.g., inventory, analytics, billing) without changing publisher code.
  - The notification consumer listens on `ecommerce.orders.notifications.queue` with routing pattern `order.*` to receive all order lifecycle events.
- **Alternatives considered**:
  - *Direct Exchange with 3 separate queues*: Creates unnecessary queue proliferation and requires 3 separate listener methods or classes.
  - *Single default queue without exchange*: Locks the system into point-to-point messaging, preventing pub-sub fanout to other subsystems.

---

### Decision 3: Event Message Contracts and Wire Serialization

- **Decision**: Publish structured, versioned event messages (`OrderEventMessage`) containing:
  - `eventId`: UUID
  - `eventType`: String (`order.created.v1`, `order.updated.v1`, `order.cancelled.v1`)
  - `orderId`: UUID
  - `orderStatus`: String (`CREATED`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`)
  - `totalAmountInCents`: Integer
  - `currency`: String (3-letter ISO)
  - `timestamp`: Instant (ISO 8601 UTC)
  - `traceId`: String
- **Rationale**:
  - Complies with Constitution Principle 4 (Contract at Boundary) and Principle 6 (Structured Observability).
  - Serialized to JSON using Micronaut Serde (`io.micronaut.serde.annotation.Serdeable`).
  - Contains correlation metadata for tracing across message boundaries.
- **Alternatives considered**:
  - *Java binary serialization*: Violates cross-language interoperability and version safety.
  - *Publishing complete Order aggregate on the wire*: Leaks internal domain representations to wire format and creates large message sizes.

---

### Decision 4: Micronaut RabbitMQ Integration Components

- **Decision**:
  1. **Publisher Client**: Declarative Micronaut RabbitMQ client `@RabbitClient("ecommerce.orders.exchange")` interface `RabbitOrderEventClient`.
  2. **Event Publisher Adapter**: `RabbitMqEventPublisherAdapter` implementing domain port `EventPublisherPort`, delegating to `RabbitOrderEventClient`.
  3. **Consumer Listener**: `@RabbitListener` class `OrderRabbitNotificationConsumer` listening on `@Queue("ecommerce.orders.notifications.queue")`.
  4. **Topology Initializer**: `@Singleton` bean `RabbitMqTopologyInitializer` using RabbitMQ's `ChannelInitializer` or connection hook to declare the exchange, queue, and binding idempotently at startup.
- **Rationale**:
  - Idiomatic Micronaut declarative programming model.
  - Clean separation: domain knows only `EventPublisherPort`, infrastructure handles `@RabbitClient` and `@RabbitListener`.
- **Alternatives considered**:
  - *Manual Channel management with raw RabbitMQ Java client*: Excess boilerplate, lacks Micronaut lifecycle management.

---

### Decision 5: TestResources Integration Testing Strategy

- **Decision**:
  - Integration test `OrderNotificationRabbitMqIntegrationTest` annotated with `@MicronautTest(environments = "test")`.
  - Micronaut TestResources automatically spins up an ephemeral `rabbitmq:3-management` Docker container during test execution.
  - Test verifies:
    1. Order creation -> `OrderCreatedEvent` published -> consumer receives message -> notification transport invoked.
    2. Order status update -> `OrderUpdatedEvent` published -> consumer receives message -> notification transport invoked.
    3. Order cancellation -> `OrderCancelledEvent` published -> consumer receives message -> notification transport invoked.
- **Rationale**:
  - Complies with Constitution Principle 5: Test real boundary instances with TestResources instead of mocking RabbitMQ.
  - Zero local setup required; works identically in local development and CI.
