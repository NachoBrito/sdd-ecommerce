# Feature Specification: Asynchronous Order Notifications via RabbitMQ

**Feature Branch**: `005-rabbitmq-order-notifications`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Input**: User description: "Notification system works right now using Micronaut events. We want this to be fully asyncrhronous using RabbitMQ: 1. The notification service should publish events to RabbitMQ when an order is created, updated or cancelled. 2. The events should be consumed by a new RabbitMQ consumer that will send the notifications. 3. The consumer is part of the same notification service. Besides unit testing, write an integration test using Micronaut test resources to spin up a RabbitMQ instance for testing."

## Clarifications

### Session 2026-10-27

- Q: How should order updates and cancellations be implemented in the domain and application layers? → A: Add status update (`updateOrderStatus`) and cancellation (`cancelOrder`) use cases and domain methods in `Order` and `OrderService` that publish `OrderUpdatedEvent` and `OrderCancelledEvent`.
- Q: How should notifications and email templates be formatted for updated and cancelled orders? → A: Parameterize the existing HTML email template to dynamically reflect the order status (`CREATED`, `UPDATED`, `CANCELLED`) and adapt the email subject accordingly (e.g. `Order Confirmation - [id]`, `Order Updated - [id]`, `Order Cancelled - [id]`).
- Q: What RabbitMQ exchange and queue topology should be configured? → A: A Topic Exchange (`ecommerce.orders.exchange`) with routing keys `order.created`, `order.updated`, `order.cancelled` bound to a single notification queue (`ecommerce.orders.notifications.queue`).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Asynchronous Notification for Created Orders (Priority: P1) 🎯 MVP

As an e-commerce customer completing a purchase, I want to receive an order confirmation notification asynchronously via an external message broker (RabbitMQ) without increasing order submission latency, so that my order completes immediately while confirmation delivery is guaranteed by reliable background message processing.

**Why this priority**: Essential MVP functionality. Transitions the existing synchronous/in-process order confirmation flow to a decoupled, durable RabbitMQ-backed asynchronous workflow.

**Independent Test**: Can be independently tested by placing an order, verifying that an order creation event is published to RabbitMQ topic exchange, and confirming that the RabbitMQ consumer picks up the message and dispatches the notification to configured transports.

**Acceptance Scenarios**:

1. **Given** a customer places an order successfully, **When** the order is saved, **Then** an `OrderCreatedEvent` message is published to the RabbitMQ exchange `ecommerce.orders.exchange` with routing key `order.created` and the checkout request returns immediately.
2. **Given** an `OrderCreatedEvent` published to RabbitMQ, **When** the notification consumer processes the message from `ecommerce.orders.notifications.queue`, **Then** it retrieves the complete order details and dispatches the order confirmation notification to all active transports.
3. **Given** the message queue consumer encounters a transient failure dispatching the notification, **Then** the message failure is isolated and does not affect the order state in the database.

---

### User Story 2 - Asynchronous Notification for Updated Orders (Priority: P2)

As an e-commerce customer whose pending order has been updated (status change), I want to receive an asynchronous notification detailing the order update, so that I am kept informed of changes to my purchase without impacting system responsiveness.

**Why this priority**: Fulfills the requirement that order updates trigger notifications via RabbitMQ. Extends asynchronous messaging beyond initial creation to subsequent order lifecycle transitions.

**Independent Test**: Can be tested independently by updating an existing order status via `updateOrderStatus`, verifying that an `OrderUpdatedEvent` is published to RabbitMQ, and verifying that the consumer handles the event and triggers an update notification.

**Acceptance Scenarios**:

1. **Given** an existing order, **When** its status is updated via `updateOrderStatus`, **Then** the updated order is persisted and an `OrderUpdatedEvent` is published to `ecommerce.orders.exchange` with routing key `order.updated`.
2. **Given** an `OrderUpdatedEvent` received from the RabbitMQ queue, **When** the consumer processes the event, **Then** it dispatches an order update notification with the updated status and subject `Order Updated - [orderId]` to configured transports.

---

### User Story 3 - Asynchronous Notification for Cancelled Orders (Priority: P3)

As an e-commerce customer whose order is cancelled, I want to receive an asynchronous cancellation notification via RabbitMQ, so that I receive immediate, auditable confirmation of the cancellation and next steps.

**Why this priority**: Fulfills the requirement that order cancellations trigger notifications via RabbitMQ. Completes the full lifecycle notification triad (created, updated, cancelled).

**Independent Test**: Can be tested independently by cancelling an existing order via `cancelOrder`, verifying that an `OrderCancelledEvent` is published to RabbitMQ, and verifying that the consumer delivers a cancellation notification.

**Acceptance Scenarios**:

1. **Given** an existing order, **When** it is cancelled via `cancelOrder`, **Then** the order status transition to `CANCELLED` is persisted and an `OrderCancelledEvent` is published to `ecommerce.orders.exchange` with routing key `order.cancelled`.
2. **Given** an `OrderCancelledEvent` received from the RabbitMQ queue, **When** the consumer processes the event, **Then** it dispatches an order cancellation notification with subject `Order Cancelled - [orderId]` to configured transports.

---

### User Story 4 - Ephemeral Integration Testing via Test Resources (Priority: P4)

As a developer and automated CI pipeline, I want an end-to-end integration test that automatically provisions an ephemeral RabbitMQ instance via Micronaut TestResources, publishes order events, and asserts consumer receipt and notification dispatch, so that the asynchronous messaging integration is verified without manual broker setup.

**Why this priority**: Explicitly mandated requirement ("write an integration test using Micronaut test resources to spin up a RabbitMQ instance for testing"). Ensures non-regression and verifiable boundary testing (Principle 5 & 10).

**Independent Test**: Can be executed via `./gradlew test` without any external running RabbitMQ service, passing automatically by spinning up an ephemeral container.

**Acceptance Scenarios**:

1. **Given** a test environment with Docker available, **When** the RabbitMQ notification integration test runs, **Then** Micronaut TestResources automatically spins up a RabbitMQ container, provisions bindings, and connects the application.
2. **Given** the running integration test, **When** order events (created, updated, cancelled) are published, **Then** the RabbitMQ consumer receives them, processes notifications, and assertions succeed.

---

### Edge Cases

- **RabbitMQ Broker Connection Outage**: If RabbitMQ is temporarily unreachable during event publication, the system must log a structured error with correlation trace ID; the primary database transaction must handle connection failure gracefully according to recovery policies.
- **Malformed Message Payload in Queue**: If a message in the RabbitMQ queue cannot be deserialized, the consumer must log the error and reject/dead-letter the message to prevent poison-pill queue blocking.
- **Consumer Processing Failure**: If a transport (such as SMTP) fails during consumer message handling, the error must be logged with diagnostic context and must not cause message reprocessing loops if unrecoverable.
- **Concurrent Order Events**: Events for the same order (created, then updated) arriving at the queue contain timestamps and status to preserve order history integrity.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST publish an asynchronous event message to RabbitMQ topic exchange `ecommerce.orders.exchange` with routing key `order.created` whenever an order is created.
- **FR-002**: The system MUST publish an asynchronous event message to RabbitMQ topic exchange `ecommerce.orders.exchange` with routing key `order.updated` whenever an order is updated.
- **FR-003**: The system MUST publish an asynchronous event message to RabbitMQ topic exchange `ecommerce.orders.exchange` with routing key `order.cancelled` whenever an order is cancelled.
- **FR-004**: Each published order event message MUST contain a unique event ID, event type identifier (`order.created.v1`, `order.updated.v1`, `order.cancelled.v1`), order ID, order status, timestamp in UTC, and correlation trace ID.
- **FR-005**: The system MUST provide a RabbitMQ consumer within the notification service that listens to the `ecommerce.orders.notifications.queue` queue bound to `ecommerce.orders.exchange` with topic pattern `order.*`.
- **FR-006**: Upon consuming an order event, the RabbitMQ consumer MUST invoke the notification dispatch logic to send notifications across all active notification transports.
- **FR-007**: The notification service MUST parameterize the HTML email template to reflect the order status (`CREATED`, `UPDATED`, `CANCELLED`) and set the corresponding email subject (`Order Confirmation - [id]`, `Order Updated - [id]`, or `Order Cancelled - [id]`).
- **FR-008**: The messaging infrastructure MUST define RabbitMQ topology with exchange `ecommerce.orders.exchange` and queue `ecommerce.orders.notifications.queue`.
- **FR-009**: The order domain and application service MUST expose inbound use cases to update order status (`UpdateOrderStatusUseCase`) and cancel an order (`CancelOrderUseCase`).
- **FR-010**: Automated integration tests MUST verify RabbitMQ publishing and consumption using Micronaut TestResources to automatically provision an ephemeral RabbitMQ broker.
- **FR-011**: All message publishing and consumption MUST adhere to Hexagonal Architecture boundaries (domain logic independent of RabbitMQ annotations).

### Key Entities *(include if feature involves data)*

- **OrderCreatedEvent**: Domain and message payload emitted when an order is first placed.
- **OrderUpdatedEvent**: Domain and message payload emitted when an existing order status is modified.
- **OrderCancelledEvent**: Domain and message payload emitted when an order is cancelled.
- **OrderNotification**: Value object representing the notification payload containing order summary details, recipient information, status, and metadata.
- **RabbitMQ Topology**: Topic exchange `ecommerce.orders.exchange` and queue `ecommerce.orders.notifications.queue` bound to routing pattern `order.*`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of order creation, update, and cancellation events are published to RabbitMQ asynchronously.
- **SC-002**: 100% of published order events are successfully received and processed by the RabbitMQ notification consumer during normal operations.
- **SC-003**: Order submission latency is reduced or decoupled from external transport delivery times.
- **SC-004**: Integration test suite successfully provisions an ephemeral RabbitMQ instance via Micronaut TestResources, executes all test scenarios, and terminates cleanly with zero manual intervention.
- **SC-005**: All code verification checks (`./gradlew check`, Checkstyle, PMD, SpotBugs, ArchUnit, Jacoco >= 80% coverage) pass with zero warnings and zero violations.
