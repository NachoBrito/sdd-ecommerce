# Implementation Plan: Asynchronous Order Notifications via RabbitMQ

**Branch**: `005-rabbitmq-order-notifications` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/005-rabbitmq-order-notifications/spec.md`

## Summary

Migrate the in-process Micronaut event notification dispatch to a fully asynchronous, durable messaging architecture using RabbitMQ. When orders are created, updated, or cancelled, domain events are published to a RabbitMQ Topic Exchange (`ecommerce.orders.exchange`). A new RabbitMQ consumer within the notification service listens on `ecommerce.orders.notifications.queue`, consumes the event messages, and triggers customer notifications across active transports. The existing HTML email template is parameterized to reflect order lifecycle status dynamically. Ephemeral integration testing is implemented using Micronaut TestResources.

## Technical Context

**Language/Version**: Java 25  
**Primary Dependencies**: Micronaut Framework 4.10.1, Micronaut RabbitMQ (`io.micronaut.rabbitmq:micronaut-rabbitmq`), Micronaut AOP, Micronaut Validation, Micronaut TestResources, Jakarta Mail API, SLF4J  
**Storage**: MariaDB (Order persistence via Micronaut Data JDBC) + RabbitMQ message broker  
**Testing**: JUnit 5, Mockito, ArchUnit, Micronaut Test, TestResources (ephemeral RabbitMQ & MariaDB containers)  
**Target Platform**: Linux / Containerized Docker (Eclipse Temurin 25 JRE base)  
**Project Type**: Single modular project (Hexagonal Architecture)  
**Performance Goals**: <10ms publication overhead, asynchronous non-blocking order notification processing  
**Constraints**: Zero warnings across Checkstyle, PMD, SpotBugs; zero ArchUnit boundary violations; strict Hexagonal Architecture compliance; no RabbitMQ annotations in domain or application packages  
**Scale/Scope**: RabbitMQ declarative client, event publisher adapter, RabbitMQ listener consumer, topology declaration, domain events & use cases, and comprehensive unit & integration tests  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **P1 Distribution**: Single deployable maintained? (RabbitMQ justified under Condition 2: Genuinely independent compute lifecycle & failure isolation for external notification transport I/O. Both publisher and consumer reside within the same modular monolith deployable).
- [x] **P2 Simplicity & Deletion**: Speculative abstractions rejected? Rule of Three respected? Module rewrite in <=1 day (<500 LOC)? (Clean declarative `@RabbitClient` and `@RabbitListener` adapters under 100 LOC each; no bespoke queue frameworks).
- [x] **P3 Explicit Dependencies**: All dependencies injected via constructors/parameters? No global singletons or import side-effects? (Explicit constructor injection across all services, publishers, and listeners).
- [x] **P4 Boundary Contracts**: Explicit versioned schemas at all boundaries (HTTP/queue/file/DB)? Domain logic isolated from wire payloads? (`OrderEventMessage` boundary DTO annotated with `@Serdeable`; JSON Schema defined in `contracts/`).
- [x] **P5 Testing Strategy**: Pure transformations tested with unit tests; boundaries tested with integration tests; zero mocks of internal code? (Pure unit tests for domain events and template rendering; integration test with real ephemeral RabbitMQ container via TestResources).
- [x] **P6 Structured Observability**: Structured events emitted with mandatory context (timestamp, event_type, request_id, user_id, tenant_id, feature_flags)? (All published event messages and consumer logs include `eventId`, `eventType`, `orderId`, `timestamp`, and `traceId`).
- [x] **P7 Recovery & Rollback**: Revertible in <5 min without code changes? Risky paths feature-flagged? Expand-then-contract migrations? (RabbitMQ transport can be toggled via `rabbitmq.enabled`; consumer failure isolates transport errors without aborting orders).
- [x] **P8 Alerting & Attention**: Alerts tied to user-visible symptoms with runbook links? Diagnostic logging for dead-lettered / unprocessable messages.
- [x] **P9 User Realization**: Definition of Done includes production deployment, live telemetry verification, and rollback readiness.
- [x] **P10 Discoverable Commands**: All developer and CI workflows runnable via unified root commands (`make build`, `make check`, `make test`, `./gradlew check`).

## Project Structure

### Documentation (this feature)

```text
specs/005-rabbitmq-order-notifications/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 output (architectural & technical decisions)
├── data-model.md        # Phase 1 output (event contracts & domain ports)
├── quickstart.md        # Phase 1 output (verification & execution guide)
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── contracts/
    ├── order-created-event.json   # Wire schema for OrderCreatedEvent
    ├── order-updated-event.json   # Wire schema for OrderUpdatedEvent
    └── order-cancelled-event.json # Wire schema for OrderCancelledEvent
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/es/nachobrito/sdd/ecommerce/
│   │   ├── domain/
│   │   │   ├── event/
│   │   │   │   ├── OrderCreatedEvent.java                     # Existing domain event
│   │   │   │   ├── OrderUpdatedEvent.java                     # New domain event
│   │   │   │   └── OrderCancelledEvent.java                   # New domain event
│   │   │   ├── model/
│   │   │   │   └── Order.java                                 # Updated with cancel() and updateStatus() methods
│   │   │   └── port/
│   │   │       ├── inbound/
│   │   │       │   ├── UpdateOrderStatusUseCase.java          # Inbound use case for status updates
│   │   │       │   └── CancelOrderUseCase.java                # Inbound use case for order cancellation
│   │   │       └── outbound/
│   │   │           └── EventPublisherPort.java                # Updated outbound port with update/cancel events
│   │   ├── application/
│   │   │   └── service/
│   │   │       └── OrderService.java                          # Implements update & cancel use cases
│   │   └── infrastructure/
│   │       └── adapter/
│   │           ├── input/
│   │           │   └── messaging/
│   │           │       └── OrderRabbitNotificationConsumer.java # RabbitMQ listener consuming order events
│   │           └── output/
│   │               ├── messaging/
│   │               │   ├── EventBusAdapter.java               # Publishes events to RabbitMQ client
│   │               │   ├── RabbitOrderEventClient.java        # Declarative Micronaut @RabbitClient
│   │               │   ├── RabbitMqTopologyInitializer.java   # Declares exchange, queue, and bindings
│   │               │   └── dto/
│   │               │       └── OrderEventMessage.java         # Serdeable JSON DTO for RabbitMQ wire messages
│   │               └── notification/
│   │                   └── smtp/
│   │                       └── SmtpHtmlTemplateRenderer.java  # Updated with dynamic status and subject
│   └── resources/
│       ├── application.yml                                    # RabbitMQ exchange/queue configurations
│       └── templates/
│           └── order-confirmation.html                        # Updated template supporting status badges
└── test/
    └── java/es/nachobrito/sdd/ecommerce/
        ├── domain/
        │   └── OrderTest.java                                 # Unit tests for updateStatus and cancel
        ├── application/service/
        │   └── OrderServiceTest.java                          # Unit tests for new order use cases
        └── infrastructure/
            └── OrderNotificationRabbitMqIntegrationTest.java  # Real RabbitMQ integration test via TestResources
```
