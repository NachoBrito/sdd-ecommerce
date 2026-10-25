# Feature Specification: Product Aggregate Updates (Client-Side ID and Price in Cents)

**Feature Branch**: `002-update-product-aggregate`  
**Created**: 2026-10-25  
**Status**: Draft  
**Input**: User description: "Two changes to the Product aggregate: 1. We use client-side id generation. Make the id a required parameter in the create product endpoint. Implement proper validation in every layer: controller, service and repository. 2. We express prices in cents: Rename the field to priceInCents. Change the type to integer. Add proper validation in every layer: controller, service and repository."

## Clarifications

### Session 2026-10-25

- Q: What HTTP response status should be returned when a client submits an `id` that already exists in the catalog? → A: HTTP 409 Conflict (`ProductAlreadyExistsException` mapped to 409).
- Q: What is the minimum allowable value for `priceInCents`? → A: 1 cent (`priceInCents >= 1`), rejecting 0 and negative numbers.
- Q: Which integer representation should be used across Java layers and database schema? → A: Java 32-bit `Integer` (allowing `@NotNull` validation in DTOs and entities) and MariaDB `INT`.
- Q: How should database schema migration handle existing decimal prices? → A: Follow Constitution Principle 7 (expand-and-contract) by adding `price_in_cents` additively, migrating `ROUND(price * 100)`, and supporting rollback.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client-Side ID Provisioning for Product Creation (Priority: P1)

As an API client or upstream ordering system, I want to assign a client-generated unique identifier (UUID) when creating a product, so that I can establish deterministic resource identities, avoid distributed coordination round trips, and achieve idempotent creation workflows.

**Why this priority**: Client-side ID generation fundamentally alters the identity contract for catalog items and enables upstream distributed systems to generate IDs before submitting creation commands.

**Independent Test**: Can be tested independently by issuing a POST `/v1/products` request containing a client-generated UUID, verifying that the product is saved with that exact ID, and verifying that omitting or sending an invalid ID fails with appropriate validation status.

**Acceptance Scenarios**:

1. **Given** a valid client-generated UUID and valid product details, **When** the client submits a POST request to `/v1/products`, **Then** the product is created with the exact provided UUID and returned with HTTP 201 Created.
2. **Given** a create product request payload missing the `id` field or containing an invalid UUID string, **When** the request reaches the controller, **Then** the request is rejected with HTTP 400 Bad Request and validation error messages detailing the missing or invalid ID.
3. **Given** a create product request with a UUID that already exists in the catalog repository, **When** the request is processed, **Then** the creation is rejected with HTTP 409 Conflict, preventing duplicate key overwrites.
4. **Given** an internal domain or service call attempting to instantiate or persist a `Product` without an ID, **Then** the domain model and repository port reject the operation with an `IllegalArgumentException` or `NullPointerException`.

---

### User Story 2 - Product Pricing Expressed in Integer Cents (Priority: P2)

As a catalog manager and e-commerce consumer, I want all product prices to be represented and stored as whole integer cents (e.g., 1999 for $19.99), so that arithmetic operations, database storage, and external API integrations avoid floating-point inaccuracies and fractional rounding ambiguities.

**Why this priority**: Expressing monetary prices as integer cents prevents precision loss and standardizes monetary representations across financial and e-commerce platforms.

**Independent Test**: Can be tested independently by creating and retrieving products with prices expressed as integer cents, verifying that valid values are accepted and persisted, and verifying that non-positive, missing, or fractional values are strictly rejected across controller, service, and repository layers.

**Acceptance Scenarios**:

1. **Given** a valid product payload with `priceInCents` set to a positive integer (e.g., `1999`), **When** the product is created and subsequently queried via GET `/v1/products/{id}`, **Then** the response reflects `priceInCents: 1999`.
2. **Given** a create product request with `priceInCents` missing, null, or zero/negative (e.g., `0` or `-50`), **When** the request reaches the controller, **Then** the request is rejected with HTTP 400 Bad Request with a clear validation message.
3. **Given** a domain operation attempting to construct a `Product` with `priceInCents <= 0` or null, **Then** the domain model compact constructor rejects the invocation with an `IllegalArgumentException`.
4. **Given** an attempt to persist a product entity with invalid `priceInCents` at the repository layer, **Then** the persistence adapter validates and aborts the operation.

---

### Edge Cases

- **Invalid UUID formats**: What happens when an API client passes a non-UUID string or malformed UUID string in the `id` field? The request must be rejected at the boundary with HTTP 400 Bad Request.
- **Duplicate ID submissions**: What happens when a client submits a creation request with an `id` that already belongs to an existing product? The service layer must detect the conflict and respond with HTTP 409 Conflict.
- **Zero or negative prices**: What happens when a client attempts to create a product with `priceInCents: 0` or negative values? All layers (controller DTO, service validation, domain model, and repository) must reject values less than 1 cent.
- **Integer overflow for large prices**: What happens when an extremely large value is passed? The field must be constrained within standard positive integer bounds (up to `Integer.MAX_VALUE`, representing over $21 million, which is well within catalog expectations).
- **Existing database records migration**: How does the database schema migration handle existing products previously stored with decimal `price`? A backward-compatible expand-and-contract migration must migrate existing decimal prices to integer cents without data loss.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST require a valid UUID `id` parameter in the create product request payload.
- **FR-002**: The controller layer MUST enforce validation on the `id` field (non-null and valid UUID format), returning HTTP 400 Bad Request if validation fails.
- **FR-003**: The service layer MUST validate that the client-supplied `id` is non-null and does not collide with an existing product in the catalog, returning HTTP 409 Conflict upon duplication.
- **FR-004**: The domain model `Product` MUST require a non-null UUID `id` in its constructor and factory methods.
- **FR-005**: The repository layer and persistence adapter MUST validate that `Product` has a non-null `id` prior to executing persistence operations.
- **FR-006**: The system MUST rename the product price field to `priceInCents` and change its data type to a 32-bit integer (`Integer` in Java).
- **FR-007**: The controller layer MUST validate that `priceInCents` is non-null and greater than or equal to 1 cent (`@Min(1)`).
- **FR-008**: The service layer and domain model MUST validate that `priceInCents` is non-null and >= 1 cent in all factory and constructor paths.
- **FR-009**: The repository layer and persistence entity MUST store `priceInCents` as an integer column (`price_in_cents`) in the database.
- **FR-010**: All outgoing REST responses (`ProductResponse`) MUST return `id` as a UUID and `priceInCents` as an integer.
- **FR-011**: Database changes MUST be applied via Liquibase migrations following the expand-and-contract pattern.

### Key Entities *(data involved in feature)*

- **Product Aggregate Root**:
  - `id`: UUID (Required, unique identifier generated client-side)
  - `sku`: String (Required, 3-32 characters matching `^[A-Z0-9-]{3,32}$`)
  - `name`: String (Required, 1-128 characters)
  - `description`: String (Optional, up to 1024 characters)
  - `priceInCents`: Integer (Required, minimum value 1)
  - `currency`: String (Required, ISO-4217 3-letter currency code)
  - `status`: ProductStatus (Required, enum: DRAFT, ACTIVE, ARCHIVED)
  - `createdAt`: Instant (Required, audit timestamp)
  - `updatedAt`: Instant (Required, audit timestamp)
- **CreateProductRequest (Input DTO)**:
  - `id`: UUID (Required, validated at boundary)
  - `sku`: String (Required, validated at boundary)
  - `name`: String (Required, validated at boundary)
  - `description`: String (Optional, validated at boundary)
  - `priceInCents`: Integer (Required, validated at boundary: min 1)
  - `currency`: String (Required, validated at boundary: 3 chars)
- **ProductResponse (Output DTO)**:
  - `id`: UUID
  - `sku`: String
  - `name`: String
  - `description`: String
  - `priceInCents`: Integer
  - `currency`: String
  - `status`: String
  - `createdAt`: Instant
  - `updatedAt`: Instant

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of product creation requests missing an `id` or providing an invalid UUID format are rejected at the REST boundary with HTTP 400 Bad Request.
- **SC-002**: 100% of product creation requests with `priceInCents` missing or less than 1 are rejected at the REST boundary with HTTP 400 Bad Request.
- **SC-003**: 100% of internal programmatic attempts to instantiate or persist invalid `id` or `priceInCents` values in service or repository layers fail with explicit validation exceptions.
- **SC-004**: 100% of successfully created products retain their client-generated UUID without any server-side ID reassignment.
- **SC-005**: All existing and newly added automated tests, static analysis checks (Checkstyle, PMD, SpotBugs), and ArchUnit rules pass with zero warnings and zero failures (`./gradlew check`).
