# Feature Specification: Orders Management

**Feature Branch**: `003-orders-management`  
**Created**: 2026-10-25  
**Status**: Draft  
**Input**: User description: "Define a new feature for the e-commerce system: orders management. The order aggregate is composed of the following entities: order (id, date, status, totalAmountInCents, currency), deliveryAddress (name, telephone, street, complement, city, state, zipCode, country), invoiceAddress (name, vatNumber, telephone, street, complement, city, state, zipCode, country), orderLine (productId, quantity, unitPriceInCents, lineTotalInCents). Implement the following use cases: Create Order: Creates a new order with the given details. Get Order: Retrieves an order by its ID. List Orders: Retrieves all orders."

## Clarifications

### Session 2026-10-25

- Q: How is the order `id` generated? → A: Client-side UUID provisioning via `CreateOrderRequest` (consistent with catalog architecture in 002, enabling idempotent order placement and zero distributed ID generation coordination).
- Q: What is the initial order status upon creation? → A: `CREATED` (representing an order that has been successfully validated and persisted in the system).
- Q: How are line totals and the total order amount validated? → A: Each order line calculates `lineTotalInCents = quantity * unitPriceInCents`. The order's `totalAmountInCents` must equal the exact sum of all line totals. Any discrepancy results in HTTP 400 Bad Request.
- Q: What are the cardinality and value constraints for order lines? → A: An order MUST contain at least one order line. Quantities must be >= 1, unit prices in cents must be >= 1, and currency must be a valid ISO-4217 3-letter code.
- Q: What HTTP response status codes are returned for duplicate IDs and missing orders? → A: Submitting an existing `id` returns HTTP 409 Conflict (`OrderAlreadyExistsException`). Querying a non-existent order by ID returns HTTP 404 Not Found (`OrderNotFoundException`).
- Q: How does the List Orders endpoint respond when no orders exist? → A: Returns HTTP 200 OK with an empty JSON array `[]`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Order (Priority: P1) 🎯 MVP

As an e-commerce buyer or checkout service, I want to submit a complete order containing delivery and invoice addresses, order lines, and currency details with a client-generated UUID, so that the order is validated, calculated, persisted, and ready for fulfillment.

**Why this priority**: Order placement is the core business transaction in an e-commerce system. Without creating orders, no retrieval or listing can occur.

**Independent Test**: Can be tested independently by submitting a POST request to `/v1/orders` with valid order details, delivery address, invoice address, and order lines, and verifying that HTTP 201 Created is returned along with the persisted order details and `CREATED` status.

**Acceptance Scenarios**:

1. **Given** a valid order payload with a unique client-generated UUID `id`, valid delivery address, valid invoice address, and valid order lines where `totalAmountInCents` equals the sum of line totals, **When** the client posts to `/v1/orders`, **Then** the order is persisted with status `CREATED`, current timestamp `date`, and returned with HTTP 201 Created and matching response body.
2. **Given** a create order payload with a duplicate `id` that already exists in the system, **When** the client posts to `/v1/orders`, **Then** the system rejects the request with HTTP 409 Conflict.
3. **Given** a create order payload with missing mandatory fields (e.g., missing street, negative quantity, zero unit price, or empty lines list), **When** the client posts to `/v1/orders`, **Then** the request is rejected with HTTP 400 Bad Request and validation error details.
4. **Given** a create order payload where `totalAmountInCents` does not match the sum of `orderLine.lineTotalInCents`, **When** the client posts to `/v1/orders`, **Then** the request is rejected with HTTP 400 Bad Request indicating total amount mismatch.

---

### User Story 2 - Get Order by ID (Priority: P2)

As a customer or customer support representative, I want to retrieve the complete details of a specific order by its unique ID, so that I can inspect the status, items, amounts, and destination addresses.

**Why this priority**: Order status lookup is essential for order tracking, confirmation pages, and customer inquiries immediately following order creation.

**Independent Test**: Can be tested independently by querying GET `/v1/orders/{id}` for an existing order ID and verifying that HTTP 200 OK is returned with complete order details, or querying an unknown UUID and verifying HTTP 404 Not Found.

**Acceptance Scenarios**:

1. **Given** an existing order with ID `123e4567-e89b-12d3-a456-426614174000`, **When** a client sends GET `/v1/orders/123e4567-e89b-12d3-a456-426614174000`, **Then** the system returns HTTP 200 OK with the full order representation including addresses and lines.
2. **Given** a valid UUID that does not correspond to any persisted order, **When** a client sends GET `/v1/orders/{id}`, **Then** the system returns HTTP 404 Not Found.
3. **Given** an invalid UUID string format in the path parameter (e.g. `abc-invalid`), **When** a client sends GET `/v1/orders/{id}`, **Then** the system returns HTTP 400 Bad Request.

---

### User Story 3 - List Orders (Priority: P3)

As an operations manager or customer service agent, I want to retrieve all orders in the system, so that I can monitor order flow and view historical activity across all orders.

**Why this priority**: Enables administrative monitoring, order history overviews, and batch reconciliation.

**Independent Test**: Can be tested independently by issuing GET `/v1/orders` after creating multiple orders, verifying that all orders are returned in the response list, and verifying that an empty database returns HTTP 200 OK with `[]`.

**Acceptance Scenarios**:

1. **Given** multiple orders exist in the database, **When** a client sends GET `/v1/orders`, **Then** the system returns HTTP 200 OK containing an array of all orders.
2. **Given** no orders exist in the database, **When** a client sends GET `/v1/orders`, **Then** the system returns HTTP 200 OK with an empty array `[]`.

---

### Edge Cases

- **Mismatched Line Totals**: A client passes `quantity: 2`, `unitPriceInCents: 500`, but specifies `lineTotalInCents: 900`. The system MUST validate that `lineTotalInCents == quantity * unitPriceInCents` and reject mismatches with HTTP 400.
- **Mismatched Total Amount**: A client submits lines totaling 2000 cents, but sets `totalAmountInCents: 1500`. The system MUST reject the creation with HTTP 400.
- **Zero or Empty Lines**: An order submitted with an empty lines list (`[]`) must be rejected with HTTP 400.
- **Negative or Zero Values**: Any quantity < 1 or unitPriceInCents < 1 must be rejected with HTTP 400.
- **Address String Boundary Limits**: Oversized strings or empty strings for mandatory address fields (name, street, city, state, zipCode, country, telephone, vatNumber) must be rejected with HTTP 400.
- **Optional Complement Field**: The `complement` field in both `deliveryAddress` and `invoiceAddress` may be null or omitted, but if provided must not exceed max character constraints (255 chars).
- **ISO Currency Validation**: Currency must be a 3-letter alphabetic string conforming to ISO-4217 (e.g., "USD", "EUR").

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow clients to create an order by supplying a unique client-generated UUID `id`, `deliveryAddress`, `invoiceAddress`, `currency`, `totalAmountInCents`, and a list of `orderLine` items.
- **FR-002**: The system MUST reject order creation with HTTP 409 Conflict if the supplied order `id` already exists.
- **FR-003**: The system MUST assign the initial status `CREATED` and set the order `date` to the current instant in UTC upon creation.
- **FR-004**: The system MUST validate that each `orderLine` has a valid `productId` (UUID), `quantity` >= 1, `unitPriceInCents` >= 1, and `lineTotalInCents` equal to `quantity * unitPriceInCents`.
- **FR-005**: The system MUST validate that an order contains at least one `orderLine`.
- **FR-006**: The system MUST validate that `totalAmountInCents` matches the sum of all `lineTotalInCents` in the order.
- **FR-007**: The system MUST validate mandatory delivery address fields: `name`, `telephone`, `street`, `city`, `state`, `zipCode`, `country`.
- **FR-008**: The system MUST validate mandatory invoice address fields: `name`, `vatNumber`, `telephone`, `street`, `city`, `state`, `zipCode`, `country`.
- **FR-009**: The system MUST allow `complement` to be null or optional in both delivery and invoice addresses.
- **FR-010**: The system MUST retrieve an order by its UUID via `GET /v1/orders/{id}`, returning HTTP 200 OK with the full aggregate structure.
- **FR-011**: The system MUST return HTTP 404 Not Found when retrieving an order by an ID that does not exist.
- **FR-012**: The system MUST retrieve all orders via `GET /v1/orders`, returning HTTP 200 OK with a list of orders (or an empty list if none exist).
- **FR-013**: The domain layer (`Order`, `DeliveryAddress`, `InvoiceAddress`, `OrderLine`) MUST be implemented as pure immutable Java records with zero framework dependencies.
- **FR-014**: All persistence operations MUST be performed via an outbound repository port and adapter against MariaDB using Micronaut Data JDBC and Liquibase schema migrations.
- **FR-015**: The system MUST emit a structured domain event `OrderCreatedEvent` upon successful order creation.

---

### Key Entities *(data involved in feature)*

- **Order (Aggregate Root)**:
  - `id`: UUID (Required, unique client-provided identifier)
  - `customerName`: String (Required, non-blank customer full name)
  - `customerEmail`: String (Required, valid email format)
  - `date`: Instant (Required, timestamp when order was placed)
  - `status`: OrderStatus (Required, enum: CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
  - `totalAmountInCents`: Integer (Required, sum of line totals, >= 1)
  - `currency`: String (Required, ISO-4217 3-letter code)
  - `deliveryAddress`: DeliveryAddress (Required value object)
  - `invoiceAddress`: InvoiceAddress (Required value object)
  - `lines`: List<OrderLine> (Required, non-empty collection)

- **DeliveryAddress (Value Object)**:
  - `name`: String (Required, 1-128 chars)
  - `telephone`: String (Required, 3-32 chars)
  - `street`: String (Required, 1-255 chars)
  - `complement`: String (Optional, nullable, max 255 chars)
  - `city`: String (Required, 1-100 chars)
  - `state`: String (Required, 1-100 chars)
  - `zipCode`: String (Required, 1-20 chars)
  - `country`: String (Required, 2-100 chars)

- **InvoiceAddress (Value Object)**:
  - `name`: String (Required, 1-128 chars)
  - `vatNumber`: String (Required, 3-32 chars)
  - `telephone`: String (Required, 3-32 chars)
  - `street`: String (Required, 1-255 chars)
  - `complement`: String (Optional, nullable, max 255 chars)
  - `city`: String (Required, 1-100 chars)
  - `state`: String (Required, 1-100 chars)
  - `zipCode`: String (Required, 1-20 chars)
  - `country`: String (Required, 2-100 chars)

- **OrderLine (Entity within Aggregate)**:
  - `productId`: UUID (Required, identifier of ordered product)
  - `quantity`: Integer (Required, >= 1)
  - `unitPriceInCents`: Integer (Required, >= 1)
  - `lineTotalInCents`: Integer (Required, calculated as quantity * unitPriceInCents)

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: API clients can complete order creation with sub-50ms p95 latency under normal operating conditions.
- **SC-002**: 100% of invalid order requests (e.g., negative price, line total mismatch, missing address) are rejected at the REST boundary with explanatory HTTP 400 messages.
- **SC-003**: 100% of order retrieval requests for existing IDs return consistent, complete aggregate state matching the created order.
- **SC-004**: Zero financial or arithmetic discrepancies: order total amounts strictly equal the sum of order line totals across all stored records.
