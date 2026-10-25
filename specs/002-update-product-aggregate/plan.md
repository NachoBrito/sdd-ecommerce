# Implementation Plan: Product Aggregate Updates (Client-Side ID and Price in Cents)

**Branch**: `002-update-product-aggregate` | **Date**: 2026-10-25 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/002-update-product-aggregate/spec.md`

## Summary

Refactor the catalog `Product` aggregate and all surrounding Hexagonal Architecture layers to enforce client-side UUID generation and store prices strictly as whole integer cents (`priceInCents`). This eliminates server-side ID coordination and floating-point/decimal rounding hazards. Comprehensive validation is introduced across each architectural layer:
- **REST Boundary**: `CreateProductRequest` mandates non-null `id` and `priceInCents >= 1` using Micronaut Validation.
- **Application Service**: `ProductService` verifies non-null inputs, validates that `id` does not collide with an existing product, and maps duplicates to HTTP 409 Conflict via `ProductAlreadyExistsException`.
- **Domain Layer**: `Product` record enforces non-null `id` and `priceInCents >= 1` in compact constructors and factory methods without framework leakage.
- **Persistence Layer**: `ProductPersistenceAdapter` and `ProductEntity` map `priceInCents` to `price_in_cents INT NOT NULL`, backed by an expand-and-contract Liquibase migration.

## Technical Context

**Language/Version**: Java 25  
**Primary Dependencies**: Micronaut Framework 4.10.1, Micronaut Data JDBC, Micronaut Validation, Micronaut TestResources, Micronaut AOP, MariaDB JDBC Driver, Liquibase  
**Storage**: MariaDB (`products` table), managed via sequential Liquibase migrations and compile-time Micronaut Data JDBC  
**Testing**: JUnit 5, REST Assured, ArchUnit, Testcontainers  
**Target Platform**: Linux / Containerized Docker (Eclipse Temurin 25 JRE base)  
**Project Type**: Single modular project (Hexagonal Architecture)  
**Performance Goals**: <50ms p95 endpoint response time, zero-allocation domain validation overhead, compile-time repository queries  
**Constraints**: Zero warnings allowed across compilation and static analysis (Checkstyle, PMD, SpotBugs); zero ArchUnit violations; backward-compatible DB migration  
**Scale/Scope**: Product aggregate refactoring across all ports, adapters, entities, DTOs, migrations, and integration test suites  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **P1 Distribution**: Single deployable maintained? (Monolithic in-process service; no microservices or external queues added).
- [x] **P2 Simplicity & Deletion**: Speculative abstractions rejected? Rule of Three respected? Module rewrite in <=1 day (<500 LOC)? (Legacy decimal `price` deleted cleanly in favor of integer `priceInCents`; direct record methods).
- [x] **P3 Explicit Dependencies**: All dependencies injected via constructors/parameters? No global singletons or import side-effects? (Explicit constructor injection in all services and adapters).
- [x] **P4 Boundary Contracts**: Explicit versioned schemas at all boundaries (HTTP/queue/file/DB)? Domain logic isolated from wire payloads? (OpenAPI 3.1 contract updated in `contracts/openapi.yaml`, validated DTOs at REST boundary, pure domain records).
- [x] **P5 Testing Strategy**: Pure transformations tested with unit tests; boundaries tested with integration tests; zero mocks of internal code; ephemeral MariaDB via TestResources?
- [x] **P6 Structured Observability**: Structured events emitted with mandatory context (timestamp, event_type, request_id, user_id, tenant_id, feature_flags)? (Existing AOP logging and domain event emission preserved).
- [x] **P7 Recovery & Rollback**: Revertible in <5 min without code changes? Risky paths feature-flagged? Expand-then-contract migrations via Liquibase? (Additive column migration `price_in_cents` with data backfill and rollback support).
- [x] **P8 Alerting & Attention**: Alerts tied to user-visible symptoms with runbook links? Health endpoints configured?
- [x] **P9 User Realization**: Definition of Done includes production deployment, live telemetry verification, and rollback readiness?
- [x] **P10 Discoverable Commands**: All developer and CI workflows runnable via unified root commands (`make build`, `make check`, `make test`)?

## Project Structure

### Documentation (this feature)

```text
specs/002-update-product-aggregate/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 output (decisions on client-side ID & cents pricing)
├── data-model.md        # Phase 1 output (updated aggregate & DTO attributes)
├── quickstart.md        # Phase 1 output (curl examples & verification steps)
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
│   │   │   │   ├── Product.java                # Updated with UUID id and int priceInCents
│   │   │   │   └── ProductStatus.java
│   │   │   ├── exception/
│   │   │   │   └── ProductAlreadyExistsException.java # Domain conflict exception
│   │   │   ├── event/
│   │   │   │   └── ProductCreatedEvent.java
│   │   │   └── port/
│   │   │       ├── inbound/
│   │   │       │   ├── CreateProductUseCase.java # Signature updated with id & priceInCents
│   │   │       │   └── GetProductUseCase.java
│   │   │       └── outbound/
│   │   │           └── ProductRepositoryPort.java
│   │   ├── application/
│   │   │   └── service/
│   │   │       └── ProductService.java         # ID uniqueness validation & cents handling
│   │   └── infrastructure/
│   │       └── adapter/
│   │           ├── input/rest/
│   │           │   ├── ProductController.java  # Controller endpoint & conflict error handler
│   │           │   └── dto/
│   │           │       ├── CreateProductRequest.java # Required id and priceInCents
│   │           │       └── ProductResponse.java      # Serializes priceInCents
│   │           └── output/persistence/
│   │               ├── ProductEntity.java            # priceInCents mapping
│   │               ├── ProductPersistenceAdapter.java # Validation before persistence
│   │               └── MicronautProductRepository.java
│   └── resources/
│       └── db/changelog/
│           ├── db.changelog-master.xml
│           └── changes/
│               ├── 001-create-products-table.xml
│               └── 002-add-price-in-cents.xml        # Expand-and-contract migration
└── test/
    └── java/es/nachobrito/sdd/ecommerce/
        ├── architecture/
        │   └── HexagonalArchitectureTest.java
        ├── domain/
        │   └── ProductTest.java                      # Unit tests for ID & price validation
        └── infrastructure/
            └── ProductControllerIntegrationTest.java # REST Assured test with client UUID & cents
```

**Structure Decision**: Preserves Hexagonal Architecture conventions inside `es.nachobrito.sdd.ecommerce`, isolating domain models from frameworks, while updating driving and driven ports and adapters.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| None | All principles satisfied without exception | Expand-and-contract migration and multi-layer validation directly implement requirements |
