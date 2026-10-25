# Antigravity Agent Guidelines: SDD E-Commerce Platform

This repository defines the REST API for the e-commerce platform using Java 25, Gradle, Micronaut 4, MariaDB, Liquibase, and Hexagonal Architecture.

All AI coding assistants working in this repository MUST strictly follow the principles below:

---

## Non-Negotiable Core Principles

### 1. Always Run the Build Command to Ensure All Checks Pass
- After making any code changes, **ALWAYS** run `./gradlew check` (or `make check`) to ensure that all tests, static analysis tools (Checkstyle, PMD, SpotBugs), and ArchUnit architectural compliance tests pass with zero warnings and zero violations.
- A task or pull request is NOT complete until all checks and tests pass green in CI and locally.

### 2. In Case of Error, Analyze the Error and Fix the Code
- When a build check or test fails:
  - Carefully read and analyze the compiler, linter, ArchUnit, or test diagnostic output.
  - Locate the exact root cause in the application, domain, or test source code and fix the code.
  - **NEVER modify project build configuration (`build.gradle`, `settings.gradle`, Checkstyle, PMD, SpotBugs rules, or compiler flags) to bypass an error without explicit user authorization.**

### 3. Apply AOP Principles for Crosscutting Concerns
- Do NOT pollute domain models or application services with crosscutting boilerplate (such as manual logging statements, tracing, security checks, or transaction boilerplate).
- Use **Aspect-Oriented Programming (AOP)** principles via Micronaut AOP (`@Around`, `@Introduction`, method interceptors) to declare and enforce crosscutting concerns cleanly at the boundary.

---

## Architectural & Coding Standards

1. **Hexagonal Architecture (Ports and Adapters)**:
   - `es.nachobrito.sdd.ecommerce.domain`: Pure business logic and domain models. Zero framework imports (no Micronaut, no Jakarta, no database drivers).
   - `es.nachobrito.sdd.ecommerce.domain.port.inbound`: Driving use-case interfaces.
   - `es.nachobrito.sdd.ecommerce.domain.port.outbound`: Driven interfaces (repositories, event publishers).
   - `es.nachobrito.sdd.ecommerce.application`: Use-case implementation services and AOP interceptors.
   - `es.nachobrito.sdd.ecommerce.infrastructure`: Framework adapters (REST controllers, Micronaut Data JDBC repositories, RabbitMQ event listeners).
   - This boundary is strictly validated on every build by ArchUnit (`HexagonalArchitectureTest`).

2. **Micronaut-Native Solutions**:
   - Prefer Micronaut Data JDBC over Hibernate/JPA.
   - Prefer Micronaut Validation over external validator engines.
   - Prefer Java records and Micronaut sourcegen over Lombok.

3. **Database Changes via Liquibase**:
   - Never write ad-hoc schema modification scripts or rely on auto-DDL.
   - All schema changes must be added as sequential, rollback-capable Liquibase changesets following the expand-and-contract pattern.

4. **Testing Standards**:
   - Unit tests test pure business transformations without mocking domain logic.
   - Integration tests test external boundaries (MariaDB, RabbitMQ) using real ephemeral instances via Micronaut TestResources.
   - Functional REST endpoints are tested end-to-end using REST Assured.

5. **Commit Messages & Versioning**:
   - Adhere to the Conventional Commits specification (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`).
   - Adhere to Semantic Versioning (`MAJOR.MINOR.PATCH`).
