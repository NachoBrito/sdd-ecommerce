# sdd-ecommerce Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-10-25

## Active Technologies
- MariaDB (`orders`, `order_lines` tables), managed via sequential Liquibase migrations (003-orders-management)
- Java 25 + Micronaut Framework 4.10.1, Micronaut AOP, Micronaut Validation, Micronaut TestResources, Jakarta Mail API, SLF4J (004-order-notifications)
- N/A (Notification service is stateless, reading orders from existing MariaDB storage via `GetOrderUseCase`) (004-order-notifications)
- Java 25 + Micronaut Framework 4.10.1, Micronaut RabbitMQ (`io.micronaut.rabbitmq:micronaut-rabbitmq`), Micronaut AOP, Micronaut Validation, Micronaut TestResources, Jakarta Mail API, SLF4J (005-rabbitmq-order-notifications)
- MariaDB (Order persistence via Micronaut Data JDBC) + RabbitMQ message broker (005-rabbitmq-order-notifications)

- Java 25 + Micronaut Framework 4.10.1, Micronaut Data JDBC, Micronaut Validation, Micronaut TestResources, Micronaut AOP, MariaDB JDBC Driver, Liquibase (002-update-product-aggregate)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 25

## Code Style

Java 25: Follow standard conventions

## Recent Changes
- 005-rabbitmq-order-notifications: Added Java 25 + Micronaut Framework 4.10.1, Micronaut RabbitMQ (`io.micronaut.rabbitmq:micronaut-rabbitmq`), Micronaut AOP, Micronaut Validation, Micronaut TestResources, Jakarta Mail API, SLF4J
- 004-order-notifications: Added Java 25 + Micronaut Framework 4.10.1, Micronaut AOP, Micronaut Validation, Micronaut TestResources, Jakarta Mail API, SLF4J
- 003-orders-management: Added Java 25 + Micronaut Framework 4.10.1, Micronaut Data JDBC, Micronaut Validation, Micronaut TestResources, Micronaut AOP, MariaDB JDBC Driver, Liquibase


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
