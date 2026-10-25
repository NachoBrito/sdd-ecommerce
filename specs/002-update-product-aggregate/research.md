# Phase 0 Technical Research: Product Aggregate Updates (Client-Side ID & Price in Cents)

**Feature**: `002-update-product-aggregate`  
**Date**: 2026-10-25  

## Research & Architecture Decisions

### Decision 1: Client-Side UUID Generation & Boundary Validation

- **Decision**: The API client generates and supplies a standard UUID string as the `id` field in `CreateProductRequest`. Validation is enforced across all three architectural layers:
  1. *Controller (REST Boundary)*: `@NotNull(message = "Product ID is required") UUID id` in `CreateProductRequest`. If invalid format or null, Micronaut Validation intercepts and returns HTTP 400 Bad Request.
  2. *Application / Service*: `ProductService.createProduct(...)` checks `productRepository.findById(id)`. If present, throws `ProductAlreadyExistsException`, which an HTTP exception handler maps to HTTP 409 Conflict.
  3. *Domain Model*: `Product` compact constructor enforces `Objects.requireNonNull(id, "Product id must not be null")`.
  4. *Persistence / Repository*: `ProductPersistenceAdapter` enforces `Objects.requireNonNull(product.id())`. The underlying MariaDB `products` table defines `id VARCHAR(36)` with a primary key constraint ensuring DB-level uniqueness.
- **Rationale**:
  - Aligns with REST best practices and client-side deterministic identity patterns.
  - Satisfies Constitution Principle 4 (Contract at the Boundary, Not in the Middle) and Principle 3 (Make Dependencies Explicit).
  - Fulfills the requirement: "Make the id a required parameter in the create product endpoint. Implement proper validation in every layer: controller, service and repository."
- **Alternatives Considered**:
  - *Server-Side Auto-Generation*: Status quo using `UUID.randomUUID()` in domain factory. Rejected because user explicitly specified client-side ID generation.
  - *Returning HTTP 400 on Duplicate ID*: Rejected in favor of HTTP 409 Conflict, which is the RFC 9110 standard for resource conflict upon duplicate entity creation.

---

### Decision 2: Expressing Price in Integer Cents

- **Decision**: Rename the property from `price` to `priceInCents` and change its type from `BigDecimal` to 32-bit `Integer` (boxed `Integer` for DTOs/entities to allow `@NotNull` validation):
  1. *Controller (REST Boundary)*: In `CreateProductRequest`:
     ```java
     @NotNull(message = "Price in cents is required")
     @Min(value = 1, message = "Price in cents must be at least 1 cent")
     Integer priceInCents
     ```
  2. *Application / Service*: Validates `priceInCents != null` and `priceInCents >= 1` before building aggregate.
  3. *Domain Model*: `Product` record holds `Integer priceInCents`, constructor enforces `if (priceInCents == null || priceInCents < 1) throw new IllegalArgumentException("Product price in cents must be at least 1");`.
  4. *Persistence / Repository*: `ProductEntity` holds `Integer priceInCents` mapped to column `price_in_cents INT NOT NULL`.
  5. *Database Schema*: Liquibase changeset adds `price_in_cents INT NOT NULL` with check constraint `>= 1`.
- **Rationale**:
  - Expressing prices in integer cents eliminates IEEE 754 floating-point inaccuracies and fractional rounding discrepancies.
  - Standard signed 32-bit `Integer` supports values up to 2,147,483,647 cents ($21,474,836.47), far exceeding standard e-commerce catalog item requirements while remaining memory-efficient.
- **Alternatives Considered**:
  - *`Long priceInCents`*: Overkill for single product unit price; user explicitly specified "Change the type to integer".
  - *Keeping `BigDecimal price` alongside `priceInCents`*: Violates Principle 2 (Optimize for Deletion, Not Extension). Dead/duplicate fields must be replaced cleanly.

---

### Decision 3: Database Schema Migration via Liquibase (Expand-and-Contract)

- **Decision**: Implement sequential, backward-compatible changeset `002-add-price-in-cents.xml` following Constitution Principle 7:
  1. Add column `price_in_cents INT` (initially nullable).
  2. Execute SQL update backfilling existing rows: `UPDATE products SET price_in_cents = CAST(ROUND(price * 100) AS INT) WHERE price_in_cents IS NULL;`.
  3. Alter column `price_in_cents` to `nullable="false"`.
  4. Drop column `price` in contract phase or maintain dual-write during phased transition.
- **Rationale**:
  - Guarantees zero downtime, zero data corruption, and clean rollback capability.
  - The table primary key `id VARCHAR(36)` is already equipped to store UUID strings directly.
- **Alternatives Considered**:
  - *Destructive drop/recreate table*: Prohibited by Constitution Principle 7.

---

### Decision 4: Exception Handling and Error Contract

- **Decision**:
  - For validation errors (null id, blank fields, priceInCents < 1), Micronaut's built-in `ConstraintViolationExceptionMapper` automatically returns HTTP 400 Bad Request with field-level details.
  - For duplicate ID conflicts, define domain exception `ProductAlreadyExistsException` and register an `@Error` handler in `ProductController` (or Micronaut `ExceptionHandler`) returning HTTP 409 Conflict with structured JSON:
    ```json
    {
      "message": "Product with ID '<uuid>' already exists",
      "status": 409
    }
    ```
- **Rationale**:
  - Clean separation between transport layer (HTTP status codes) and domain invariants.
