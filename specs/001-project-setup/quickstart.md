# Quickstart: E-Commerce REST API Foundation

**Feature Branch**: `001-project-setup`  
**Date**: 2026-10-25  

## Prerequisites

- **Java Development Kit**: JDK 25 installed and on `PATH` (or configured via SDKMAN `sdk use java 25-graalce`).
- **Docker**: Docker engine running (required for Micronaut TestResources to spin up MariaDB and RabbitMQ).
- **Make** (optional, recommended for discoverable root commands).

## Quick Commands

All tasks can be executed via `make` or directly via `./gradlew`:

```bash
# Display help and all available commands
make help

# Run all quality checks, style gates, and static analysis (Checkstyle, PMD, SpotBugs, ArchUnit)
make check

# Execute test suite (Unit, Integration, REST Assured, and ArchUnit tests)
make test

# Build and package the application jar
make build

# Run application locally with ephemeral test resources (MariaDB & RabbitMQ)
make dev

# Build the production Docker image
make docker-build
```

## Direct Gradle Commands

```bash
# Check code quality and style
./gradlew check

# Run tests
./gradlew test

# Clean build
./gradlew clean build

# Run application with test resources
./gradlew run
```

## Verifying Strict Quality Gates

Any violation of Checkstyle, PMD, SpotBugs, or ArchUnit architectural rules will immediately fail `./gradlew check`.

To test that quality gates actively prevent violations:
- Attempting to introduce a domain dependency on an infrastructure class fails `HexagonalArchitectureTest` (ArchUnit).
- Introducing an unused private field or bad style triggers a PMD/Checkstyle error and breaks the build.
