# Implementation Plan: E-Commerce REST API Project Foundation

**Branch**: `001-project-setup` | **Date**: 2026-10-25 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-project-setup/spec.md`

## Summary

Establish the complete project foundation, build automation, architectural boundary enforcement, and strict quality verification pipelines for an e-commerce platform REST API. The stack leverages Java 25, Gradle 8.9+, Micronaut 4.10+, Micronaut Data JDBC with MariaDB, Liquibase migrations, RabbitMQ event bus, ArchUnit architectural testing, Checkstyle, PMD, SpotBugs, Docker, and Micrometer OpenTelemetry observation. All tools are configured with strict industry-standard rules where any violation fails the build.

## Technical Context

**Language/Version**: Java 25 (targeting GraalVM / OpenJDK 25 toolchain)  
**Primary Dependencies**: Micronaut Framework 4.10.1, Micronaut Data JDBC, Micronaut Validation, Micronaut TestResources, Micronaut RabbitMQ, Micronaut AOP, Micronaut Management, MariaDB JDBC Driver, Liquibase  
**Storage**: MariaDB (relational database), managed via Liquibase migrations and Micronaut Data JDBC  
**Testing**: JUnit 5, Mockito, REST Assured, ArchUnit, Testcontainers (managed via Micronaut TestResources)  
**Target Platform**: Linux / Containerized Docker (Eclipse Temurin / GraalVM 25 JRE base)  
**Project Type**: Single project (modular monolithic REST API service with Hexagonal Architecture)  
**Performance Goals**: Sub-second application startup, <50ms p95 endpoint response time, zero-reflection compile-time dependency injection  
**Constraints**: Zero warnings allowed across compilation and static analysis; fail build immediately on any rule violation; strictly non-root Docker container  
**Scale/Scope**: Foundation setup, core Product aggregate slice, full build automation, quality gating, and developer workflow commands  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **P1 Distribution**: Single deployable maintained? (Vertical scaling monolithic deployable; RabbitMQ messaging configured for decoupled domain events without distributed microservice sprawl).
- [x] **P2 Simplicity & Deletion**: Speculative abstractions rejected? Rule of Three respected? Module rewrite in <=1 day (<500 LOC per module)?
- [x] **P3 Explicit Dependencies**: All dependencies injected via constructors/parameters? No global singletons or import side-effects? (Micronaut compile-time DI with explicit constructor injection throughout).
- [x] **P4 Boundary Contracts**: Explicit versioned schemas at all boundaries (HTTP/queue/file/DB)? Domain logic isolated from wire payloads? (OpenAPI 3.1 contract, versioned `/v1` endpoints, dedicated DTOs mapped at adapter boundary).
- [x] **P5 Testing Strategy**: Pure transformations tested with unit tests; boundaries tested with integration tests; zero mocks of internal code; real ephemeral MariaDB via TestResources?
- [x] **P6 Structured Observability**: Structured events emitted with mandatory context (timestamp, event_type, request_id, user_id, tenant_id, feature_flags) via Micrometer and OpenTelemetry?
- [x] **P7 Recovery & Rollback**: Revertible in <5 min without code changes? Risky paths feature-flagged? Expand-then-contract migrations via Liquibase?
- [x] **P8 Alerting & Attention**: Alerts tied to user-visible symptoms with runbook links? Health endpoints configured at `/health`?
- [x] **P9 User Realization**: Definition of Done includes production deployment, live telemetry verification, and rollback readiness?
- [x] **P10 Discoverable Commands**: All developer and CI workflows runnable via unified root commands (`make build`, `make check`, `make test`, `make run`, `make docker-build`, `make help`)?

## Project Structure

### Documentation (this feature)

```text
specs/001-project-setup/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Technology decisions & analysis
├── data-model.md        # Foundational data model & Liquibase schema
├── quickstart.md        # Developer setup & run instructions
├── contracts/
│   └── openapi.yaml     # OpenAPI 3.1 specification for foundational API
└── tasks.md             # Task breakdown (generated in Phase 2)
```

### Source Code (repository root)

```text
.
├── Makefile                        # Unified command runner (P10)
├── build.gradle                    # Root build script with strict quality plugins
├── settings.gradle                 # Gradle settings (sdd-ecommerce)
├── gradle/
│   └── wrapper/                    # Gradle wrapper binaries & configuration
├── Dockerfile                      # Multi-stage production container build
├── AGENTS.md                       # AI assistant guidelines and governance
├── config/
│   ├── checkstyle/
│   │   └── checkstyle.xml          # Strict Google-based Checkstyle rules
│   ├── pmd/
│   │   └── pmd.xml                 # Strict PMD ruleset
│   └── spotbugs/
│       └── exclude.xml             # SpotBugs configuration filter
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── ecommerce/
    │   │           ├── Application.java
    │   │           ├── domain/                 # Pure domain layer (no framework deps)
    │   │           │   ├── model/
    │   │           │   │   ├── Product.java
    │   │           │   │   └── ProductStatus.java
    │   │           │   ├── event/
    │   │           │   │   └── ProductCreatedEvent.java
    │   │           │   └── port/
    │   │           │       ├── inbound/        # Use cases (driving ports)
    │   │           │       │   ├── CreateProductUseCase.java
    │   │           │       │   └── GetProductUseCase.java
    │   │           │       └── outbound/       # Repositories & publishers (driven ports)
    │   │           │           ├── ProductRepositoryPort.java
    │   │           │           └── EventPublisherPort.java
    │   │           ├── application/            # Application services orchestrating use cases
    │   │           │   └── service/
    │   │           │       ├── ProductService.java
    │   │           │       └── aop/            # AOP interceptors for logging & tracing
    │   │           │           └── LoggingInterceptor.java
    │   │           └── infrastructure/         # Adapters & framework implementations
    │   │               ├── adapter/
    │   │               │   ├── input/
    │   │               │   │   └── rest/       # REST controllers, DTOs
    │   │               │   │       ├── ProductController.java
    │   │               │   │       ├── dto/
    │   │               │   │       │   ├── CreateProductRequest.java
    │   │               │   │       │   └── ProductResponse.java
    │   │               │   │       └── mapper/
    │   │               │   └── output/
    │   │               │       ├── persistence/ # Micronaut Data JDBC repository
    │   │               │       │   ├── ProductEntity.java
    │   │               │       │   ├── MicronautProductRepository.java
    │   │               │       │   └── ProductPersistenceAdapter.java
    │   │               │       └── messaging/   # Event bus publisher adapter
    │   │               │           └── EventBusAdapter.java
    │   │               └── config/             # Micronaut beans, telemetry config
    │   └── resources/
    │       ├── application.yml                 # Base application configuration
    │       ├── logback.xml                     # Structured logging configuration
    │       └── db/
    │           └── changelog/
    │               ├── db.changelog-master.xml # Liquibase master changelog
    │               └── changes/
    │                   └── 001-create-products-table.xml
    └── test/
        ├── java/
        │   └── com/
        │       └── ecommerce/
        │           ├── architecture/           # ArchUnit architecture tests
        │           │   └── HexagonalArchitectureTest.java
        │           ├── domain/                 # Pure unit tests (no mocks)
        │           │   └── ProductTest.java
        │           └── infrastructure/         # Integration & REST Assured tests
        │               ├── ProductControllerIntegrationTest.java
        │               └── EventBusIntegrationTest.java
        └── resources/
            └── application-test.yml            # Test configuration (TestResources enabled)
```

**Structure Decision**: Hexagonal Architecture within a single modular project root (`src/main/java/es/nachobrito/sdd/ecommerce/`), strictly segregating `domain` (zero framework dependencies), `application` (orchestration, AOP), and `infrastructure` (REST controllers, Micronaut Data JDBC persistence, RabbitMQ event bus adapter).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| None | All principles satisfied without exception | Baseline monolithic architecture chosen |
