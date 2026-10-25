# Implementation Plan: Orders Management

**Branch**: `003-orders-management` | **Date**: 2026-10-25 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/003-orders-management/spec.md`

## Summary

Implement the core `Order` aggregate and associated use cases (Create Order, Get Order by ID, List Orders) within the Hexagonal Architecture pattern. The aggregate contains the `Order` root entity, `DeliveryAddress` and `InvoiceAddress` value objects, and `OrderLine` child entities.
Key architectural highlights:
- **REST Boundary**: `OrderController` exposes `POST /v1/orders`, `GET /v1/orders/{id}`, and `GET /v1/orders`. Input validation mandates non-null fields, positive amounts in cents, and arithmetic consistency.
- **Application Services**: `OrderService` implements inbound use case ports (`CreateOrderUseCase`, `GetOrderUseCase`, `ListOrdersUseCase`), orchestrating domain entities, repositories, and structured event emission.
- **Domain Layer**: Pure immutable records (`Order`, `DeliveryAddress`, `InvoiceAddress`, `OrderLine`, `OrderStatus`) enforcing core invariants in compact constructors without framework leakage.
- **Persistence Layer**: `OrderPersistenceAdapter` implements `OrderRepositoryPort` with Micronaut Data JDBC repositories (`MicronautOrderRepository`, `MicronautOrderLineRepository`) mapping to `orders` and `order_lines` tables created via Liquibase migrations.

## Technical Context

**Language/Version**: Java 25  
**Primary Dependencies**: Micronaut Framework 4.10.1, Micronaut Data JDBC, Micronaut Validation, Micronaut TestResources, Micronaut AOP, MariaDB JDBC Driver, Liquibase  
**Storage**: MariaDB (`orders`, `order_lines` tables), managed via sequential Liquibase migrations  
**Testing**: JUnit 5, REST Assured, ArchUnit, Testcontainers  
**Target Platform**: Linux / Containerized Docker (Eclipse Temurin 25 JRE base)  
**Project Type**: Single modular project (Hexagonal Architecture)  
**Performance Goals**: <50ms p95 endpoint response time, zero-allocation domain validation overhead, compile-time JDBC mapping  
**Constraints**: Zero warnings allowed across compilation and static analysis (Checkstyle, PMD, SpotBugs); zero ArchUnit violations; full Liquibase rollback support  
**Scale/Scope**: Order aggregate implementation across ports, adapters, entities, DTOs, migrations, and integration test suites  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **P1 Distribution**: Single deployable maintained? (Monolithic in-process service; no microservices or external queues added).
- [x] **P2 Simplicity & Deletion**: Speculative abstractions rejected? Rule of Three respected? Module rewrite in <=1 day (<500 LOC)? (Cohesive records; direct mapper methods; value objects embedded directly into `orders` table without intermediate join tables).
- [x] **P3 Explicit Dependencies**: All dependencies injected via constructors/parameters? No global singletons or import side-effects? (Explicit constructor injection in all services and persistence adapters).
- [x] **P4 Boundary Contracts**: Explicit versioned schemas at all boundaries (HTTP/queue/file/DB)? Domain logic isolated from wire payloads? (OpenAPI 3.1 contract in `contracts/openapi.yaml`, validated DTOs at REST boundary, pure domain records).
- [x] **P5 Testing Strategy**: Pure transformations tested with unit tests; boundaries tested with integration tests; zero mocks of internal code; ephemeral MariaDB via TestResources?
- [x] **P6 Structured Observability**: Structured events emitted with mandatory context (timestamp, event_type, request_id, user_id, tenant_id, feature_flags)? (Reuses `@StructuredLog` interceptor and domain event emission for `OrderCreatedEvent`).
- [x] **P7 Recovery & Rollback**: Revertible in <5 min without code changes? Risky paths feature-flagged? Expand-then-contract migrations via Liquibase? (Additive table creation in `003-create-orders-tables.xml` with explicit `<rollback>` statements).
- [x] **P8 Alerting & Attention**: Alerts tied to user-visible symptoms with runbook links? Health endpoints configured?
- [x] **P9 User Realization**: Definition of Done includes production deployment, live telemetry verification, and rollback readiness?
- [x] **P10 Discoverable Commands**: All developer and CI workflows runnable via unified root commands (`make build`, `make check`, `make test`)?

## Project Structure

### Documentation (this feature)

```text
specs/003-orders-management/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 output (architectural & technical decisions)
├── data-model.md        # Phase 1 output (aggregate records & schema definitions)
├── quickstart.md        # Phase 1 output (curl examples & verification steps)
├── checklists/
│   └── requirements.md  # Specification quality checklist
├── contracts/
│   └── openapi.yaml     # Phase 1 output (OpenAPI 3.1 contract)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/es/nachobrito/sdd/ecommerce/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Order.java                     # Order aggregate root record
│   │   │   │   ├── OrderStatus.java               # Enum: CREATED, CONFIRMED, etc.
│   │   │   │   ├── DeliveryAddress.java           # Value object record
│   │   │   │   ├── InvoiceAddress.java            # Value object record
│   │   │   │   └── OrderLine.java                 # Entity record
│   │   │   ├── exception/
│   │   │   │   ├── OrderNotFoundException.java     # 404 domain exception
│   │   │   │   ├── OrderAlreadyExistsException.java # 409 domain exception
│   │   │   │   └── InvalidOrderException.java     # 400 domain exception
│   │   │   ├── event/
│   │   │   │   └── OrderCreatedEvent.java          # Domain event record
│   │   │   └── port/
│   │   │       ├── inbound/
│   │   │       │   ├── CreateOrderUseCase.java     # Driving use case
│   │   │       │   ├── GetOrderUseCase.java        # Driving use case
│   │   │       │   └── ListOrdersUseCase.java      # Driving use case
│   │   │       └── outbound/
│   │   │           └── OrderRepositoryPort.java    # Driven port
│   │   ├── application/
│   │   │   └── service/
│   │   │       └── OrderService.java               # Application service orchestrator
│   │   └── infrastructure/
│   │       └── adapter/
│   │           ├── input/rest/
│   │           │   ├── OrderController.java        # REST controller
│   │           │   └── dto/
│   │           │       ├── CreateOrderRequest.java # Request body DTO
│   │           │       ├── OrderResponse.java      # Response DTO
│   │           │       ├── DeliveryAddressDto.java # Address DTO
│   │           │       ├── InvoiceAddressDto.java  # Address DTO
│   │           │       └── OrderLineDto.java       # Order line DTO
│   │           └── output/persistence/
│   │               ├── OrderEntity.java            # MariaDB orders entity
│   │               ├── OrderLineEntity.java        # MariaDB order_lines entity
│   │               ├── OrderPersistenceAdapter.java# Adapter implementing port
│   │               ├── MicronautOrderRepository.java # Micronaut Data JDBC repo
│   │               └── MicronautOrderLineRepository.java # Micronaut Data JDBC repo
│   └── resources/
│       └── db/changelog/
│           ├── db.changelog-master.xml
│           └── changes/
│               └── 003-create-orders-tables.xml    # Liquibase table creation
└── test/
    └── java/es/nachobrito/sdd/ecommerce/
        ├── domain/
        │   └── OrderTest.java                      # Domain unit tests
        └── infrastructure/
            └── OrderControllerIntegrationTest.java # REST Assured integration tests
```
