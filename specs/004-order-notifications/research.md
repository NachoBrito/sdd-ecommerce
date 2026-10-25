# Architectural & Technical Research: Order Notifications

**Feature Branch**: `004-order-notifications`  
**Date**: 2026-10-25  
**Spec**: [spec.md](./spec.md)

---

## Decision 1: In-Process Event-Driven Orchestration

### Decision
Use Micronaut's in-process event bus (`ApplicationEventPublisher<Object>` / `@EventListener`) to trigger order notifications upon receipt of `OrderCreatedEvent`.

### Rationale
- **Constitution Principle 1 (Do Not Distribute by Default)**: Keeps notification processing in-process within the modular monolith. No external message broker (RabbitMQ/Kafka) is introduced for internal event notification.
- **Constitution Principle 6 (Emit Structured Events, Derive Everything Else)**: The order creation workflow already emits `OrderCreatedEvent`. The notification domain subscribes to this domain event without modifying `OrderService`.
- **Decoupled Lifecycle**: The notification service retrieves the full `Order` aggregate via `GetOrderUseCase` and dispatches it independently.

### Alternatives Considered
- *Direct Call from `OrderService`*: Would tightly couple order creation use case to notification logic, violating single responsibility and Hexagonal Architecture boundaries.
- *External Queue Broker (RabbitMQ)*: Rejected per Constitution Principle 1. No multi-team boundary or scale overflow justifies an external queue for order email notifications at this stage.

---

## Decision 2: Pluggable Transport Abstraction (Rule of Three Compliant)

### Decision
Define a driven port `NotificationTransportPort` in the domain/application layer:
```java
public interface NotificationTransportPort {
    String name();
    boolean isEnabled();
    void sendOrderNotification(OrderNotification notification);
}
```
`NotificationService` injects `List<NotificationTransportPort>` via standard Micronaut constructor dependency injection, iterating through all active transports.

### Rationale
- **Requirement 1 Fulfillment**: Directly enables multiple transports with SMTP as the initial implementation.
- **Constitution Principle 2 (Optimize for Deletion, Not Extension)**: Minimalist, decision-ready interface with no speculative generic hierarchies or dynamic plugin loaders. Adding or removing a transport is a single class addition/deletion.
- **Constitution Principle 3 (Make Dependencies Explicit)**: Injected as a typed `List<NotificationTransportPort>` in `NotificationService` constructor.

### Alternatives Considered
- *Single Hardcoded SMTP Service*: Violates Requirement 1 ("accepts multiple transports, SMTP initially").
- *Complex Generic Plugin System*: Violates Principle 2. Avoid complex dynamic classloading; standard DI list injection is robust and compile-time verified.

---

## Decision 3: Transport-Specific HTML Template Rendering

### Decision
Each transport encapsulates its own template and rendering logic. The SMTP transport includes `SmtpHtmlTemplateRenderer`, which loads an HTML template from classpath resources (`templates/order-confirmation.html`) and substitutes placeholder tokens (`{{orderId}}`, `{{customerName}}`, `{{date}}`, `{{orderLines}}`, `{{totalAmount}}`, `{{currency}}`, `{{deliveryAddress}}`, `{{invoiceAddress}}`) with HTML-escaped values.

### Rationale
- **Requirement 3 Fulfillment**: "Each transport uses its own template. The SMTP transport will have an html template."
- **Isolation**: A future SMS transport can use a 160-character plain-text template, while SMTP uses rich responsive HTML, without any shared template coupling.
- **Security**: Strict HTML escaping on all dynamic fields (customer name, addresses) prevents HTML and cross-site injection attacks in email clients.
- **Lightweight**: Zero external heavy template engine dependencies needed for transactional receipt emails.

### Alternatives Considered
- *Shared Common Template*: Violates Requirement 3. Different transports require fundamentally different formats (HTML vs. plain text vs. JSON payload).
- *Thymeleaf / Freemarker*: Adds heavy third-party dependencies, extra configuration, and potential memory overhead. A clean, focused template renderer in under 150 lines of code satisfies Constitution Principle 2.

---

## Decision 4: SMTP Delivery Adapter

### Decision
Implement `SmtpNotificationTransport` in the infrastructure output adapter layer (`es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp`). It integrates with Java/Jakarta Mail or Micronaut Email and reads configuration properties (`smtp.host`, `smtp.port`, `smtp.from`, `smtp.username`, `smtp.password`, `smtp.enabled`).

### Rationale
- Standard, robust email delivery across local development, staging, and production SMTP relays (SendGrid, Mailgun, AWS SES, or internal Postfix).
- Configurable via `application.yml` with sensible defaults for local testing.

### Alternatives Considered
- *External REST Email APIs (SendGrid/Mailgun direct HTTP)*: Rejected because requirement specifically mandates SMTP initially.

---

## Decision 5: Fault Tolerance and Error Handling

### Decision
Any network or authentication error occurring during transport execution (such as SMTP connection failure, invalid recipient, or socket timeout) is caught, logged with full error and correlation context via `@StructuredLog` / SLF4J, and suppressed from aborting the order creation workflow.

### Rationale
- **Constitution Principle 7 (Recovery Over Prevention)**: An email failure must never invalidate or roll back an already-committed financial transaction (customer order).
- **Constitution Principle 6 (Structured Observability)**: Detailed failure logs allow ops to alert, diagnose, and trigger re-transmission if necessary.
