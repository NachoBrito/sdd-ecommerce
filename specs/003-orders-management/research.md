# Research: Orders Management Architecture & Technical Decisions

**Feature**: Orders Management  
**Branch**: `003-orders-management`  
**Date**: 2026-10-25  

## Decision 1: Relational Schema & Aggregate Persistence in MariaDB

### Context
The `Order` aggregate consists of the root entity `Order`, two value objects (`DeliveryAddress` and `InvoiceAddress`), and a collection of entities (`OrderLine`). We need a schema that maintains relational integrity, optimal read/write performance, and clean mapping to Hexagonal Architecture entities.

### Decision
- Store the `Order` aggregate root and its embedded value objects (`DeliveryAddress`, `InvoiceAddress`) in a single table: `orders`.
- Address fields are embedded directly into `orders` with prefixes:
  - Delivery: `delivery_name`, `delivery_telephone`, `delivery_street`, `delivery_complement`, `delivery_city`, `delivery_state`, `delivery_zip_code`, `delivery_country`
  - Invoice: `invoice_name`, `invoice_vat_number`, `invoice_telephone`, `invoice_street`, `invoice_complement`, `invoice_city`, `invoice_state`, `invoice_zip_code`, `invoice_country`
- Store `OrderLine` records in a child table: `order_lines` with columns `id VARCHAR(36) PRIMARY KEY`, `order_id VARCHAR(36) NOT NULL`, `product_id VARCHAR(36) NOT NULL`, `quantity INT NOT NULL`, `unit_price_in_cents INT NOT NULL`, `line_total_in_cents INT NOT NULL`.
- Foreign key: `order_lines.order_id` references `orders.id` with `ON DELETE CASCADE`.

### Rationale
- Embedding value objects in the aggregate root table adheres to Domain-Driven Design (DDD): addresses have no independent identity or lifecycle outside the order.
- Eliminates unnecessary multi-table JOINs for address data during order retrieval.
- Preserves referential integrity for line items with an indexed foreign key.

### Alternatives Considered
- *Separate addresses table*: Adds join complexity and treats value objects as entities without any architectural benefit.
- *JSON column for addresses or order lines*: Fails relational schema validation, lacks column-level indexing, and violates Constitution Principle 4 (strict schema at boundaries).

---

## Decision 2: Monetary Value Representation and Consistency Enforcement

### Context
Monetary calculations in e-commerce can introduce rounding errors if floating-point representations are used. Feature 002 established `priceInCents` as whole integers (`INT` in MariaDB, `Integer` in Java).

### Decision
- Represent all prices and totals as 32-bit positive integers (`Integer` in Java, `INT` in MariaDB).
- Calculate `lineTotalInCents = quantity * unitPriceInCents`.
- Calculate `totalAmountInCents = sum(lineTotalInCents)`.
- Enforce validation at two distinct gates:
  1. *REST Boundary (`CreateOrderRequest`)*: Micronaut Validation (`@NotNull`, `@Min(1)`) and custom cross-field check to ensure provided totals match line computations.
  2. *Domain Aggregate (`Order` compact constructor)*: Enforces `totalAmountInCents == lines.stream().mapToInt(OrderLine::lineTotalInCents).sum()`.

### Rationale
- Completely eliminates floating-point precision loss.
- Standardizes monetary representation across catalog and order domains.
- Compact constructor invariants guarantee that an `Order` aggregate can never exist in an inconsistent financial state.

### Alternatives Considered
- *BigDecimal*: High memory overhead, rounding mode complexity, inconsistency with Product aggregate in Feature 002.
- *Server-computed total only (ignoring client total)*: Validating client total against server computation protects against client-side display desynchronization or tampering.

---

## Decision 3: Inbound & Outbound Ports in Hexagonal Architecture

### Context
The application adheres strictly to Hexagonal Architecture validated by ArchUnit.

### Decision
- **Inbound Ports**:
  - `CreateOrderUseCase`: `Order createOrder(CreateOrderCommand command)`
  - `GetOrderUseCase`: `Order getOrder(UUID id)`
  - `ListOrdersUseCase`: `List<Order> listOrders()`
- **Outbound Ports**:
  - `OrderRepositoryPort`: `void save(Order order)`, `Optional<Order> findById(UUID id)`, `List<Order> findAll()`
  - `EventPublisherPort`: `void publish(Object event)` (reusing existing port)
- **Domain Exceptions**:
  - `OrderNotFoundException` (mapped to HTTP 404 in controller)
  - `OrderAlreadyExistsException` (mapped to HTTP 409 in controller)
  - `InvalidOrderException` (mapped to HTTP 400 in controller)

### Rationale
- Clear use-case boundary interfaces separating HTTP controllers from application services.
- Repositories return domain models (`Order`), keeping domain models 100% framework-free.

---

## Decision 4: Liquibase Migration Sequence & Rollback

### Context
Database schema updates must follow Constitution Principle 7 and 10.

### Decision
- Create `src/main/resources/db/changelog/changes/003-create-orders-tables.xml` containing:
  1. `createTable tableName="orders"`
  2. `createTable tableName="order_lines"`
  3. `addForeignKeyConstraint` linking `order_lines.order_id` to `orders.id`
  4. Explicit `<rollback>` actions dropping tables.
- Register changeset in `src/main/resources/db/changelog/db.changelog-master.xml`.

### Rationale
- Guarantees automated, repeatable schema provisioning across local, test, and CI environments without auto-DDL.
