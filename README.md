# SDD Ecommerce Platform

A production-grade, event-driven REST API for an e-commerce platform built with **Java 25**, **Micronaut 4**, **MariaDB**, **RabbitMQ**, and **Hexagonal Architecture** (Ports and Adapters).

---

## Table of Contents

- [Features & Tech Stack](#features--tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [How to Build](#how-to-build)
- [How to Test & Verify](#how-to-test--verify)
  - [Run Tests](#run-tests)
  - [Run Static Analysis & Linters](#run-static-analysis--linters)
  - [Architecture Compliance](#architecture-compliance)
- [How to Run Locally](#how-to-run-locally)
  - [Development Mode (with TestResources)](#development-mode-with-testresources)
  - [Configuring the Server Port](#configuring-the-server-port)
  - [Running with Docker](#running-with-docker)
- [Testing with a REST Client](#testing-with-a-rest-client)
- [Project Structure](#project-structure)
- [Contribution & Commit Conventions](#contribution--commit-conventions)

---

## Features & Tech Stack

- **Language & Runtime**: Java 25 (enforced via Gradle toolchain).
- **Framework**: Micronaut 4.10.1 (Netty runtime, ahead-of-time compile processing, native validation).
- **Architecture**: Hexagonal Architecture (Domain layer has 0 external framework dependencies).
- **Persistence**: MariaDB via Micronaut Data JDBC & HikariCP, managed by Liquibase schema migrations.
- **Messaging**: Event-driven domain events with Micronaut internal event bus and RabbitMQ adapter.
- **Ephemeral Environments**: Micronaut TestResources automatically provisions ephemeral MariaDB and RabbitMQ containers for local development and testing.
- **Observability**: Micrometer, OpenTelemetry, Prometheus metrics exporter (`/prometheus`), and management health probes (`/health`).
- **Quality Gates**: Strict ArchUnit, Checkstyle, PMD (7.x), and SpotBugs (zero tolerance / fail on any violation).
- **Containerization**: Multi-stage, non-root user Docker container (`eclipse-temurin:25`).

---

## Architecture

The project adheres strictly to **Hexagonal Architecture (Ports & Adapters)**:

```
es.nachobrito.sdd.ecommerce
├── domain
│   ├── model                    # Pure domain models and aggregates (Zero external frameworks)
│   └── port
│       ├── inbound              # Driving use-case interfaces (CreateProductUseCase, GetProductUseCase)
│       └── outbound             # Driven port interfaces (ProductRepositoryPort, EventPublisherPort)
├── application
│   ├── aop                      # Micronaut AOP cross-cutting concerns (e.g., @StructuredLog)
│   └── service                  # Use-case orchestrators and domain service implementations
└── infrastructure
    ├── adapter
    │   ├── input/rest           # REST API Controllers and Request/Response DTOs
    │   ├── output/messaging     # Event Bus and RabbitMQ publishing adapters
    │   └── output/persistence   # Micronaut Data JDBC entities and repository adapters
    └── config                   # Infrastructure-level framework configuration
```

Strict boundaries are tested on every build by ArchUnit (`HexagonalArchitectureTest`).

---

## Prerequisites

Before building or running the service, ensure the following are installed:

1. **Java Development Kit (JDK 25)**:
   - Verify with `java -version`.
   - Recommended via [SDKMAN!](https://sdkman.io/):
     ```bash
     sdk install java 25-graalce
     # or
     sdk install java 25-tem
     ```
2. **Docker Engine**:
   - Docker must be running to enable **Micronaut TestResources** (which automatically spins up MariaDB and RabbitMQ test containers on demand).
3. **Make** (Optional, but recommended for shortcut commands):
   - Linux: `sudo apt install make` (Debian/Ubuntu) or `sudo dnf install make` (Fedora).

---

## How to Build

A `Makefile` is provided for discoverability. You can also run `./gradlew` commands directly.

### Assemble the Application

```bash
# Using Make
make build

# Or directly using Gradle
./gradlew build -x test
```

### Build Executable Shadow (Fat) JAR

```bash
./gradlew shadowJar -x test
```
The executable artifact will be located in `build/libs/sdd-ecommerce-<version>-all.jar`.

### Build Docker Container

```bash
# Using Make
make docker-build

# Or directly with Docker
docker build -t sdd-ecommerce:latest .
```

---

## How to Test & Verify

The project enforces automated test execution and static analysis gates. A build will fail if any test fails or any linter finds a violation.

### Run Tests

Executes unit tests, integration tests (using REST Assured and ephemeral TestResources containers), and ArchUnit tests:

```bash
# Using Make
make test

# Or directly using Gradle
./gradlew test
```

Test reports are generated at:
- `build/reports/tests/test/index.html`

### Run Static Analysis & Linters

Runs Checkstyle, PMD, SpotBugs, and ArchUnit verification:

```bash
# Using Make
make check

# Or directly using Gradle
./gradlew check
```

Static analysis reports:
- **Checkstyle**: `build/reports/checkstyle/main.html`
- **PMD**: `build/reports/pmd/main.html`
- **SpotBugs**: `build/reports/spotbugs/main.html`

### Architecture Compliance

ArchUnit verifies architectural compliance in `src/test/java/es/nachobrito/sdd/ecommerce/architecture/HexagonalArchitectureTest.java`:
1. `domain` package has zero imports from Micronaut, Jakarta, Spring, or relational drivers.
2. Inbound ports are only invoked by application or adapter layers.
3. Outbound ports are only implemented by infrastructure adapters.
4. Services in `application` do not depend directly on infrastructure adapters.

---

## How to Run Locally

### Development Mode (with TestResources)

When started locally, Micronaut TestResources automatically detects that MariaDB and RabbitMQ are required, pulls the necessary containers, applies Liquibase migrations, and binds the data sources seamlessly without manual database setup.

```bash
# Using Make
make dev

# Or directly using Gradle
./gradlew run
```

### Configuring the Server Port

By default, the application listens on port `8080`. You can configure a custom port using the `MICRONAUT_SERVER_PORT` environment variable or Gradle `--args`:

```bash
# Using an environment variable
MICRONAUT_SERVER_PORT=8085 make dev

# Or with Gradle arguments
./gradlew run --args="-micronaut.server.port=8085"
```

### Running with Docker

To run the container built via `make docker-build`:

```bash
docker run --rm -p 8080:8080 \
  -e MICRONAUT_ENVIRONMENTS=prod \
  -e DATASOURCES_DEFAULT_URL=jdbc:mariadb://<host>:3306/<db> \
  -e DATASOURCES_DEFAULT_USERNAME=<user> \
  -e DATASOURCES_DEFAULT_PASSWORD=<password> \
  -e RABBITMQ_ADDRESSES_0=<rabbit_host>:5672 \
  sdd-ecommerce:latest
```

---

## Testing with a REST Client

Once the application is running (e.g. on `http://localhost:8080`), you can test the endpoints using Postman, Insomnia, VS Code REST Client, or `curl`:

### 1. Health & Readiness Check

```http
GET http://localhost:8080/health
Accept: application/json
```

```bash
curl -i http://localhost:8080/health
```

Expected Response:
```json
{
  "status": "UP"
}
```

### 2. Prometheus Metrics

```bash
curl -i http://localhost:8080/prometheus
```

### 3. Create a Product

```http
POST http://localhost:8080/v1/products
Content-Type: application/json

{
  "sku": "SKU-PROD-001",
  "name": "Ergonomic Mechanical Keyboard",
  "description": "Custom mechanical keyboard with hot-swappable switches",
  "price": 149.99,
  "currency": "USD"
}
```

```bash
curl -i -X POST http://localhost:8080/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKU-PROD-001",
    "name": "Ergonomic Mechanical Keyboard",
    "description": "Custom mechanical keyboard with hot-swappable switches",
    "price": 149.99,
    "currency": "USD"
  }'
```

Expected Response (`201 Created`):
```json
{
  "id": "1",
  "sku": "SKU-PROD-001",
  "name": "Ergonomic Mechanical Keyboard",
  "description": "Custom mechanical keyboard with hot-swappable switches",
  "price": 149.99,
  "currency": "USD",
  "status": "ACTIVE",
  "createdAt": "2026-10-25T08:30:00Z"
}
```

### 4. List All Products

```http
GET http://localhost:8080/v1/products
Accept: application/json
```

```bash
curl -i http://localhost:8080/v1/products
```

Expected Response (`200 OK`):
```json
[
  {
    "id": "1",
    "sku": "SKU-PROD-001",
    "name": "Ergonomic Mechanical Keyboard",
    "description": "Custom mechanical keyboard with hot-swappable switches",
    "price": 149.99,
    "currency": "USD",
    "status": "ACTIVE",
    "createdAt": "2026-10-25T08:30:00Z"
  }
]
```

### 5. Get Product by ID

```http
GET http://localhost:8080/v1/products/1
Accept: application/json
```

```bash
curl -i http://localhost:8080/v1/products/1
```

---

## Project Structure

```
.
├── Makefile                          # Discoverable developer commands
├── build.gradle                      # Gradle dependencies, compiler flags, and tool configurations
├── settings.gradle                   # Root project name and Gradle plugins
├── version.txt                       # Semantic version tracker
├── Dockerfile                        # Multi-stage non-root container definition
├── config/
│   ├── checkstyle/checkstyle.xml     # Checkstyle ruleset
│   ├── pmd/pmd.xml                   # PMD ruleset
│   └── spotbugs/exclude.xml          # SpotBugs exclusions
├── specs/                            # Spec-Kit feature specifications and contracts
│   └── 001-project-setup/
│       ├── spec.md                   # Feature requirements and user stories
│       ├── plan.md                   # Implementation plan and architecture rules
│       ├── tasks.md                  # Detailed task breakdown
│       └── contracts/openapi.yaml    # OpenAPI 3.1 specification for the REST API
└── src/
    ├── main/
    │   ├── java/es/nachobrito/sdd/ecommerce/
    │   │   ├── Application.java
    │   │   ├── application/          # Use-case services & AOP logging interceptors
    │   │   ├── domain/               # Core models, aggregates, and ports
    │   │   └── infrastructure/       # Controllers, entities, repositories, and event bus
    │   └── resources/
    │       ├── application.yml       # Default runtime configuration
    │       ├── logback.xml           # Structured JSON logging configuration
    │       └── db/changelog/         # Liquibase changelogs (XML)
    └── test/
        ├── java/es/nachobrito/sdd/ecommerce/
        │   ├── architecture/         # ArchUnit rule tests
        │   ├── domain/               # Domain unit tests
        │   └── infrastructure/       # REST Assured & EventBus integration tests
        └── resources/
            └── application-test.yml  # Test profile configuration
```

---

## Contribution & Commit Conventions

This repository enforces **Conventional Commits** via a Git hook:

```
<type>(<optional scope>): <description>

Example: feat(catalog): add product creation endpoint
```

Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.

To install the git commit hook locally:

```bash
./gradlew installGitHooks
```

Before pushing or opening a pull request, ensure all tests and quality checks pass:

```bash
make check
```
