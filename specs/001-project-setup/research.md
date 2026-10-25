# Research: E-Commerce REST API Project Foundation

**Feature Branch**: `001-project-setup`  
**Date**: 2026-10-25  
**Spec**: [spec.md](./spec.md)  

## Technical Decisions & Analysis

### 1. Java 25 & Gradle Build Toolchain

- **Decision**: Configure Java 25 with Gradle 8.9+ using explicit Java toolchains (`jvmToolchain(25)`).
- **Rationale**: 
  - Standardizes compilation and runtime targeting Java 25 features (virtual threads, records, pattern matching, modern language constructs).
  - Explicit toolchain ensures reproducible builds regardless of ambient shell JVM settings.
  - Gradle `options.compilerArgs` configured with `-parameters`, `-Werror`, and `-Xlint:all` (strict compilation).
- **Alternatives Considered**:
  - Java 21 LTS: Modern LTS, but user explicitly mandated Java 25.
  - Maven: Rejected per user instruction specifying Gradle.

### 2. Micronaut Framework Version & Plugins

- **Decision**: Use Micronaut 4.10.1 (latest stable 4.x) using the Micronaut Gradle Plugin (`io.micronaut.application` version 4.4.4 / `io.micronaut.test-resources` version 4.4.4).
- **Rationale**:
  - Provides ahead-of-time (AOT) compilation, zero-reflection dependency injection, minimal memory footprint, and sub-second startup.
  - Compatible with Java 25 runtime and compile-time annotation processing.
  - Directly supports Micronaut Data JDBC, Micronaut Validation, Micronaut TestResources, and Micronaut Management.
- **Alternatives Considered**:
  - Spring Boot 3: Higher reflection overhead, slower startup, not requested.
  - Quarkus: Good alternatives, but user specified Micronaut.

### 3. Persistence: Micronaut Data JDBC & MariaDB

- **Decision**: Use `io.micronaut.data:micronaut-data-jdbc` with `org.mariadb.jdbc:mariadb-java-client` and `io.micronaut.sql:micronaut-jdbc-hikari`.
- **Rationale**:
  - User strictly instructed: "Always prefer Micronaut-native solutions over alternatives: Micronaut Data instead of Hibernate/JPA".
  - Micronaut Data precomputes SQL queries at compilation time with zero reflection, avoiding JPA/Hibernate session caching complexities and runtime overhead.
  - HikariCP provides production-grade JDBC connection pooling.
- **Alternatives Considered**:
  - Hibernate/JPA: Explicitly forbidden by requirement 16.
  - R2DBC: Reactive driver, but JDBC is simpler, robust, and aligns with standard transactional boundaries while virtual threads in Java 25 deliver high throughput without reactive complexity.

### 4. Database Migrations: Liquibase

- **Decision**: Integrate `io.micronaut.liquibase:micronaut-liquibase` with master changelog `src/main/resources/db/changelog/db.changelog-master.xml` (or YAML).
- **Rationale**:
  - Version-controlled, declarative, and rollback-capable database schema management.
  - Seamlessly integrates with Micronaut startup lifecycle: automatically migrates schemas on application or test boot.
  - Enforces expand-and-contract migration patterns per Constitution Principle 7.
- **Alternatives Considered**:
  - Flyway: Reliable, but user explicitly requested Liquibase.

### 5. Local Test Infrastructure: Micronaut TestResources

- **Decision**: Enable `io.micronaut.testresources` Gradle plugin with Testcontainers-backed MariaDB and RabbitMQ resources.
- **Rationale**:
  - Micronaut TestResources automatically spins up isolated Docker containers for MariaDB and RabbitMQ during tests and local development (`./gradlew testResourcesService`).
  - Zero manual infrastructure setup required for developers (satisfies Constitution Principle 10).
  - Automatically injects ephemeral JDBC URLs, ports, credentials, and connection settings into Micronaut environment properties.
- **Alternatives Considered**:
  - Manual `docker-compose up`: Requires manual steps, susceptible to port collisions and configuration drift.
  - In-memory H2: Does not match MariaDB dialect, masking syntax, transaction, and concurrency incompatibilities (violates Constitution Principle 5).

### 6. Event-Driven Architecture & Event Bus

- **Decision**: Implement an internal Application Event Bus (`io.micronaut.context.event.ApplicationEventPublisher`) coupled with RabbitMQ integration (`io.micronaut.rabbitmq:micronaut-rabbitmq`) for asynchronous distributed messaging.
- **Rationale**:
  - Allows in-process decoupled event dispatching (domain events) with immediate transaction boundaries.
  - RabbitMQ handles durable, asynchronous cross-service messaging when required, backed by TestResources during tests.
  - Aligns with Constitution Principle 1 (modular in-process events as primary, external messaging when boundaries require).
- **Alternatives Considered**:
  - Apache Kafka: Heavier resource footprint for local development and test resources compared to RabbitMQ.

### 7. Hexagonal Architecture (Ports and Adapters)

- **Decision**: Enforce a 3-layer package structure within a single deployable:
  ```text
  es.nachobrito.sdd.ecommerce/
  ├── domain/             # Core business models, value objects, domain logic, domain ports (no framework deps)
  │   ├── model/
  │   └── port/
  │       ├── inbound/    # Primary/driving ports (use cases interfaces)
  │       └── outbound/   # Secondary/driven ports (repository & event publisher interfaces)
  ├── application/        # Application services orchestrating use cases, transactions, AOP
  │   └── service/
  └── infrastructure/     # Adapters implementing or driving ports (framework/driver dependencies)
      ├── adapter/
      │   ├── input/
      │   │   └── rest/   # HTTP controllers, DTOs, request validation
      │   └── output/
      │       ├── persistence/ # Micronaut Data JDBC repositories, entity mappers
      │       └── messaging/   # RabbitMQ listeners and event producers
      └── config/         # Micronaut beans, factory configurations
  ```
- **Rationale**:
  - Strictly isolates business rules from frameworks, transport layers, and databases.
  - Domain layer remains 100% pure Java without any Micronaut or third-party annotations (satisfies Constitution Principle 4 & 5).

### 8. Architectural Compliance: ArchUnit

- **Decision**: Configure ArchUnit (`com.tngtech.archunit:archunit-junit5:1.3.0`) in `src/test/java/.../architecture/HexagonalArchitectureTest.java`.
- **Rationale**:
  - Statically analyzes compiled bytecode to guarantee:
    1. Domain classes do not depend on Application, Infrastructure, or external libraries.
    2. Inbound and outbound ports are only located in designated port packages.
    3. Controllers reside only in `infrastructure.adapter.input.rest` and only depend on inbound ports.
    4. Repositories reside only in `infrastructure.adapter.output.persistence` and implement outbound ports.
  - Runs on every `./gradlew test` and fails the build immediately on any violation.
- **Alternatives Considered**:
  - Code review enforcement: Subjective and error-prone; ArchUnit provides deterministic automated gating.

### 9. Code Quality Gates: Checkstyle, PMD, SpotBugs

- **Decision**: Configure Gradle core plugins `checkstyle`, `pmd`, and third-party plugin `com.github.spotbugs` with strict configurations:
  - **Checkstyle**: Google Java Style Guide rules (`config/checkstyle/checkstyle.xml`), `maxWarnings = 0`, `ignoreFailures = false`.
  - **PMD**: Clean Code, Best Practices, Design, and Error Prone rulesets (`config/pmd/pmd.xml`), `consoleOutput = true`, `ignoreFailures = false`.
  - **SpotBugs**: `com.github.spotbugs` plugin, `effort = "max"`, `reportLevel = "low"`, `ignoreFailures = false`.
  - Enforced as dependencies of the root `check` task so `./gradlew check` executes all of them.
- **Rationale**:
  - Fulfills user instruction: "Configure every tool with strict rules following industry standards. Fail the build if any rule is violated. Run all the checks on every build."

### 10. Conventional Commits & Semantic Versioning

- **Decision**: Configure commit message validation script/task enforcing the Conventional Commits specification (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`, `perf:`), and establish Semantic Versioning (`major.minor.patch`) via `version.txt` / Gradle versioning.
- **Rationale**:
  - Ensures clean, automated changelog generation and deterministic release versioning.

### 11. Containerization: Docker

- **Decision**: Provide a multi-stage Dockerfile (`Dockerfile`) based on Eclipse Temurin 25 / GraalVM 25 runtime image:
  - Stage 1: Build & test verification (`./gradlew check assemble`).
  - Stage 2: Minimal distroless or lightweight JRE runtime, non-root user (`USER 10001:10001`), exposed port 8080, healthcheck directive on `/health`.
- **Rationale**:
  - Secure by default, minimal image footprint, fast container startup.

### 12. Observability: Micrometer & OpenTelemetry

- **Decision**: Include `io.micronaut.micrometer:micronaut-micrometer-core`, `io.micronaut.micrometer:micronaut-micrometer-registry-prometheus`, and `io.micronaut.tracing:micronaut-tracing-opentelemetry-http`.
- **Rationale**:
  - Automatic collection of HTTP metrics, JVM statistics, connection pool metrics, and distributed tracing.
  - Exposes standardized `/prometheus` and `/health` endpoints.
  - Emits high-cardinality trace IDs and span IDs on all incoming requests (satisfies Constitution Principle 6).

### 13. Cross-Cutting Concerns: AOP Principles

- **Decision**: Implement cross-cutting concerns (structured logging, method auditing, transaction boundaries) using Micronaut AOP (`io.micronaut.aop:micronaut-aop`) with custom around/interceptor annotations (e.g., `@StructuredLog`, `@Audited`).
- **Rationale**:
  - User explicitly mandated: "Apply AOP principles for crosscutting concerns (logging, security, transactions, etc)".
  - AOP keeps domain logic and use case services cleanly decoupled from repetitive operational boilerplate.
