# Quickstart & Verification Guide: Asynchronous Order Notifications via RabbitMQ

**Feature**: `005-rabbitmq-order-notifications`  
**Date**: 2026-10-27  

## Prerequisites

- **Java**: OpenJDK / Temurin 25 installed
- **Docker**: Docker daemon running (required by Micronaut TestResources to spin up MariaDB and RabbitMQ containers)

## Executing Verification Commands

### 1. Run the Full Build & Verification Suite
Executes unit tests, integration tests (with ephemeral MariaDB and RabbitMQ), Checkstyle, PMD, SpotBugs, ArchUnit, and Jacoco coverage checks:
```bash
./gradlew check
```

### 2. Run Only the RabbitMQ Integration Test
To run just the asynchronous RabbitMQ notification integration test:
```bash
./gradlew test --tests "es.nachobrito.sdd.ecommerce.infrastructure.OrderNotificationRabbitMqIntegrationTest"
```

### 3. Verify Static Analysis Tools Individually
```bash
# Checkstyle code formatting and style validation
./gradlew checkstyleMain checkstyleTest

# PMD source code analyzer
./gradlew pmdMain pmdTest

# SpotBugs static bytecode analyzer
./gradlew spotbugsMain spotbugsTest
```

## Local Development Execution

When running the application locally via `./gradlew run`:
- Micronaut TestResources automatically boots a local MariaDB container and RabbitMQ container (`rabbitmq:3-management`).
- RabbitMQ web management console is available on `http://localhost:<mapped-port>` with credentials `guest`/`guest`.
- The topic exchange `ecommerce.orders.exchange` and queue `ecommerce.orders.notifications.queue` are provisioned automatically at startup.
