# Tasks: Orders Management

**Input**: Design documents from `/specs/003-orders-management/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

## Format: `[TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Explicit file paths included in all task descriptions

## Path Conventions

Single project repository layout:
- `src/main/java/es/nachobrito/sdd/ecommerce/...`
- `src/main/resources/...`
- `src/test/java/es/nachobrito/sdd/ecommerce/...`
- `src/test/resources/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration registration and contract synchronization

- [X] T001 Register Liquibase changeset `003-create-orders-tables.xml` in `src/main/resources/db/changelog/db.changelog-master.xml`
- [X] T002 [P] Sync OpenAPI 3.1 contract for orders management in `specs/003-orders-management/contracts/openapi.yaml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared domain exceptions, domain models, database schema, and persistence infrastructure

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 [P] Create Liquibase migration `003-create-orders-tables.xml` for `orders` and `order_lines` tables with rollback in `src/main/resources/db/changelog/changes/003-create-orders-tables.xml`
- [X] T004 [P] Create domain exceptions `OrderNotFoundException`, `OrderAlreadyExistsException`, and `InvalidOrderException` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/exception/OrderNotFoundException.java`
- [X] T005 [P] Create domain event `OrderCreatedEvent` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/event/OrderCreatedEvent.java`
- [X] T006 [P] Create domain model records `DeliveryAddress`, `InvoiceAddress`, `OrderLine`, `OrderStatus` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/model/DeliveryAddress.java`
- [X] T007 [P] Create domain aggregate root record `Order` with compact constructor invariant validations in `src/main/java/es/nachobrito/sdd/ecommerce/domain/model/Order.java`
- [X] T008 [P] Create outbound port `OrderRepositoryPort` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/port/outbound/OrderRepositoryPort.java`
- [X] T009 [P] Create persistence entities `OrderEntity` and `OrderLineEntity` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/persistence/OrderEntity.java`
- [X] T010 [P] Create Micronaut Data JDBC repositories `MicronautOrderRepository` and `MicronautOrderLineRepository` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/persistence/MicronautOrderRepository.java`
- [X] T011 Implement `OrderPersistenceAdapter` implementing `OrderRepositoryPort` with mapping between domain records and database entities in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/persistence/OrderPersistenceAdapter.java`

**Checkpoint**: Foundation ready - domain aggregate, persistence entities, repositories, and migration structure ready.

---

## Phase 3: User Story 1 - Create Order (Priority: P1) 🎯 MVP

**Goal**: Deliver order creation across all Hexagonal Architecture layers with strict validation, arithmetic checks, and event emission.

**Independent Test**: Issue POST `/v1/orders` with valid payload, verify 201 Created and persisted order; verify invalid payloads return 400 Bad Request; verify duplicate ID returns 409 Conflict.

### Tests for User Story 1

- [X] T012 [P] [US1] Create unit tests for `Order` domain aggregate invariants (validations, totals, addresses, line calculations) in `src/test/java/es/nachobrito/sdd/ecommerce/domain/OrderTest.java`

### Implementation for User Story 1

- [X] T013 [P] [US1] Create inbound use case port `CreateOrderUseCase` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/CreateOrderUseCase.java`
- [X] T014 [P] [US1] Create boundary DTOs `DeliveryAddressDto`, `InvoiceAddressDto`, `OrderLineDto`, and `CreateOrderRequest` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/rest/dto/CreateOrderRequest.java`
- [X] T015 [P] [US1] Create boundary response DTO `OrderResponse` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/rest/dto/OrderResponse.java`
- [X] T016 [US1] Implement `CreateOrderUseCase` in `OrderService` validating ID uniqueness, persisting order, and publishing `OrderCreatedEvent` in `src/main/java/es/nachobrito/sdd/ecommerce/application/service/OrderService.java`
- [X] T017 [US1] Implement `OrderController` endpoint `POST /v1/orders` with `@Error` handlers mapping `OrderAlreadyExistsException` (409) and validation errors (400) in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/rest/OrderController.java`
- [X] T018 [US1] Create REST Assured integration test for order creation, validation rejection (400), and duplicate ID conflict (409) in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/OrderControllerIntegrationTest.java`

**Checkpoint**: User Story 1 complete and independently verifiable as an MVP.

---

## Phase 4: User Story 2 - Get Order by ID (Priority: P2)

**Goal**: Deliver order lookup by ID across all layers with full aggregate retrieval and 404 handling.

**Independent Test**: Query GET `/v1/orders/{id}` for an existing order and verify 200 OK; query a non-existent UUID and verify 404 Not Found.

### Implementation for User Story 2

- [X] T019 [P] [US2] Create inbound use case port `GetOrderUseCase` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/GetOrderUseCase.java`
- [X] T020 [US2] Implement `getOrder` method in `OrderService` querying `OrderRepositoryPort` and throwing `OrderNotFoundException` if absent in `src/main/java/es/nachobrito/sdd/ecommerce/application/service/OrderService.java`
- [X] T021 [US2] Add `GET /v1/orders/{id}` endpoint and `@Error` handler for `OrderNotFoundException` (404) to `OrderController` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/rest/OrderController.java`
- [X] T022 [US2] Add REST Assured integration test verifying order lookup by ID (200 OK) and missing order (404 Not Found) in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/OrderControllerIntegrationTest.java`

**Checkpoint**: User Story 2 complete with independently verifiable retrieval by ID.

---

## Phase 5: User Story 3 - List Orders (Priority: P3)

**Goal**: Deliver order listing across all layers returning a list of orders (or an empty list if none exist).

**Independent Test**: Query GET `/v1/orders` and verify 200 OK with an array of orders; verify 200 OK with `[]` on an empty catalog.

### Implementation for User Story 3

- [X] T023 [P] [US3] Create inbound use case port `ListOrdersUseCase` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/ListOrdersUseCase.java`
- [X] T024 [US3] Implement `listOrders` method in `OrderService` querying `OrderRepositoryPort.findAll()` in `src/main/java/es/nachobrito/sdd/ecommerce/application/service/OrderService.java`
- [X] T025 [US3] Add `GET /v1/orders` endpoint to `OrderController` returning `List<OrderResponse>` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/rest/OrderController.java`
- [X] T026 [US3] Add REST Assured integration test verifying listing all orders and handling empty lists in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/OrderControllerIntegrationTest.java`

**Checkpoint**: User Story 3 complete with independently verifiable order listing.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Architecture validation, quality checks, and clean test suite execution

- [X] T027 [P] Verify ArchUnit package isolation rules remain clean for orders management in `src/test/java/es/nachobrito/sdd/ecommerce/architecture/HexagonalArchitectureTest.java`
- [X] T028 Run full verification suite (`./gradlew check` and `make check`) to ensure 100% compliance across all checks and tests

---

## Dependencies

- Phase 1 (Setup) and Phase 2 (Foundational) must complete before any User Story.
- User Story 1 (P1 - Create Order) is the MVP and prerequisite for user-facing order data.
- User Story 2 (P2 - Get Order) depends on Phase 2 & foundational adapter.
- User Story 3 (P3 - List Orders) depends on Phase 2 & foundational adapter.
- Phase 6 (Polish) runs after all User Stories are implemented.

## Implementation Strategy

1. **MVP First**: Complete Phase 1, Phase 2, and Phase 3 (US1) to have a fully functional order creation system.
2. **Incremental Delivery**: Add Phase 4 (US2: Get Order by ID) and Phase 5 (US3: List Orders) sequentially.
3. **Quality & ArchUnit**: Verify hexagonal boundaries with ArchUnit and run `./gradlew check`.
