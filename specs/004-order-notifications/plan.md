# Implementation Plan: Order Notifications

**Branch**: `004-order-notifications` | **Date**: 2026-10-25 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/004-order-notifications/spec.md`

## Summary

Implement an extensible, event-driven notification service in the e-commerce system adhering to Hexagonal Architecture. The notification service listens for `OrderCreatedEvent` domain events published when an order is created, retrieves the full order details via `GetOrderUseCase`, and dispatches formatted notifications to configured transports.
Initial delivery implements:
1. **Multiple Transports Registry**: `NotificationTransportPort` driven port allowing multiple transports (SMTP initially, extensible to SMS/push).
2. **Dedicated Template Rendering**: Each transport manages its own medium-specific template. The SMTP transport includes `SmtpHtmlTemplateRenderer` using an HTML template (`templates/order-confirmation.html`) with XSS-safe variable interpolation.
3. **Fault Isolation**: External delivery failures (SMTP network issues) are logged with full correlation context and do not roll back or abort the customer's order placement (Principle 7).

## Technical Context

**Language/Version**: Java 25  
**Primary Dependencies**: Micronaut Framework 4.10.1, Micronaut AOP, Micronaut Validation, Micronaut TestResources, Jakarta Mail API, SLF4J  
**Storage**: N/A (Notification service is stateless, reading orders from existing MariaDB storage via `GetOrderUseCase`)  
**Testing**: JUnit 5, Mockito, ArchUnit, Micronaut Test  
**Target Platform**: Linux / Containerized Docker (Eclipse Temurin 25 JRE base)  
**Project Type**: Single modular project (Hexagonal Architecture)  
**Performance Goals**: <50ms HTML template rendering, asynchronous/non-blocking event dispatch from order creation  
**Constraints**: Zero warnings across Checkstyle, PMD, SpotBugs; zero ArchUnit boundary violations; strict HTML escaping on all interpolated template fields; no external queue infrastructure (Principle 1)  
**Scale/Scope**: In-process notification service, SMTP adapter, HTML template renderer, event listener, unit tests, and integration tests  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **P1 Distribution**: Single deployable maintained? (In-process event listener consuming `OrderCreatedEvent` via Micronaut's internal application event publisher; zero external message brokers or daemons introduced).
- [x] **P2 Simplicity & Deletion**: Speculative abstractions rejected? Rule of Three respected? Module rewrite in <=1 day (<500 LOC)? (Concise `NotificationTransportPort` interface with 3 methods; clean localized HTML template renderer under 150 LOC).
- [x] **P3 Explicit Dependencies**: All dependencies injected via constructors/parameters? No global singletons or import side-effects? (Explicit constructor injection across `NotificationService`, `OrderCreatedNotificationListener`, and `SmtpNotificationTransport`).
- [x] **P4 Boundary Contracts**: Explicit versioned schemas at all boundaries (HTTP/queue/file/DB)? Domain logic isolated from wire payloads? (Domain records `OrderNotification`, `NotificationAddress`, `NotificationLineItem`; JSON Schema for `OrderCreatedEvent` in `contracts/order-created-event.json`).
- [x] **P5 Testing Strategy**: Pure transformations tested with unit tests; boundaries tested with integration tests; zero mocks of internal code? (Pure unit tests for HTML template rendering and variable escaping; integration tests for transport dispatching and event listener).
- [x] **P6 Structured Observability**: Structured events emitted with mandatory context (timestamp, event_type, request_id, user_id, tenant_id, feature_flags)? (Structured logging with trace ID, order ID, and transport name on dispatch and delivery).
- [x] **P7 Recovery & Rollback**: Revertible in <5 min without code changes? Risky paths feature-flagged? Expand-then-contract migrations? (Transport failure isolated: email failure never rolls back order creation; SMTP transport configurable/toggleable via `smtp.enabled: true/false`).
- [x] **P8 Alerting & Attention**: Alerts tied to user-visible symptoms with runbook links? Error logs flagged with actionable severity for transport delivery failures?
- [x] **P9 User Realization**: Definition of Done includes production deployment, live telemetry verification, and rollback readiness?
- [x] **P10 Discoverable Commands**: All developer and CI workflows runnable via unified root commands (`make build`, `make check`, `make test`, `./gradlew check`)?

## Project Structure

### Documentation (this feature)

```text
specs/004-order-notifications/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 output (architectural & technical decisions)
├── data-model.md        # Phase 1 output (notification data model & port contracts)
├── quickstart.md        # Phase 1 output (verification & execution guide)
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── contracts/
    └── order-created-event.json # Boundary event contract
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/es/nachobrito/sdd/ecommerce/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── OrderNotification.java                 # Notification data model record
│   │   │   │   ├── NotificationAddress.java               # Formatted address record
│   │   │   │   └── NotificationLineItem.java              # Item summary record
│   │   │   └── port/
│   │   │       ├── inbound/
│   │   │       │   └── SendOrderNotificationUseCase.java   # Driving inbound use case
│   │   │       └── outbound/
│   │   │           └── NotificationTransportPort.java      # Driven outbound transport port
│   │   ├── application/
│   │   │   └── service/
│   │   │       └── NotificationService.java               # Application service orchestrator
│   │   └── infrastructure/
│   │       └── adapter/
│   │           ├── input/
│   │           │   └── messaging/
│   │           │       └── OrderCreatedNotificationListener.java # Event listener for OrderCreatedEvent
│   │           └── output/
│   │               └── notification/
│   │                   └── smtp/
│   │                       ├── SmtpNotificationTransport.java    # SMTP transport implementation
│   │                       ├── SmtpHtmlTemplateRenderer.java     # Dedicated HTML template engine
│   │                       ├── SmtpSender.java                   # Low-level mail sender interface
│   │                       └── SmtpConfiguration.java            # Config properties for SMTP
│   └── resources/
│       └── templates/
│           └── order-confirmation.html                           # SMTP HTML email template
└── test/
    └── java/es/nachobrito/sdd/ecommerce/
        ├── domain/model/
        │   └── OrderNotificationTest.java                        # Unit tests for notification model
        ├── application/service/
        │   └── NotificationServiceTest.java                      # Service unit tests
        └── infrastructure/adapter/
            ├── input/messaging/
            │   └── OrderCreatedNotificationListenerTest.java     # Event listener integration test
            └── output/notification/smtp/
                ├── SmtpHtmlTemplateRendererTest.java             # HTML rendering & escaping tests
                └── SmtpNotificationTransportTest.java            # SMTP transport tests
```
