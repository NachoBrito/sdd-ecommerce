# Tasks: Asynchronous Order Notifications via RabbitMQ

**Input**: Design documents from `/specs/005-rabbitmq-order-notifications/`  
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/  

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: RabbitMQ configuration, wire DTO, declarative client, and topology initialization

- [X] T001 Configure RabbitMQ exchange, queue, and routing keys in `src/main/resources/application.yml` and `src/test/resources/application-test.yml`
- [X] T002 [P] Define boundary wire DTO `OrderEventMessage` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/dto/OrderEventMessage.java`
- [X] T003 [P] Define declarative Micronaut RabbitMQ client `RabbitOrderEventClient` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/RabbitOrderEventClient.java`
- [X] T004 Implement RabbitMQ topology initializer `RabbitMqTopologyInitializer` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/RabbitMqTopologyInitializer.java` to declare `ecommerce.orders.exchange` topic exchange, `ecommerce.orders.notifications.queue`, and `order.*` binding

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure and ports that MUST be complete before user story execution

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Update outbound port `EventPublisherPort` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/port/outbound/EventPublisherPort.java` with publish methods for `OrderUpdatedEvent` and `OrderCancelledEvent`
- [X] T006 [P] Implement `RabbitMqEventPublisherAdapter` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/EventBusAdapter.java` to publish order events to `RabbitOrderEventClient`
- [X] T007 [P] Parameterize `order-confirmation.html` and update `SmtpHtmlTemplateRenderer` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpHtmlTemplateRenderer.java` and `src/main/resources/templates/order-confirmation.html` to dynamically render order status and adapt subject lines
- [X] T008 Implement `OrderRabbitNotificationConsumer` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/messaging/OrderRabbitNotificationConsumer.java` listening on `ecommerce.orders.notifications.queue` and delegating to `SendOrderNotificationUseCase`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Asynchronous Notification for Created Orders (Priority: P1) 🎯 MVP

**Goal**: Deliver end-to-end asynchronous notification dispatch for newly created orders via RabbitMQ.

**Independent Test**: Can be tested independently by publishing `OrderCreatedEvent`, verifying delivery to `ecommerce.orders.notifications.queue`, and observing notification dispatch to registered transports.

- [X] T009 [P] [US1] Unit test for publishing `OrderCreatedEvent` via `EventBusAdapter` in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/EventBusAdapterTest.java`
- [X] T010 [P] [US1] Unit test for `OrderRabbitNotificationConsumer` processing `OrderCreatedEvent` in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/messaging/OrderRabbitNotificationConsumerTest.java`
- [X] T011 [US1] Wire `OrderService.createOrder` to publish `OrderCreatedEvent` to RabbitMQ via `EventPublisherPort` in `src/main/java/es/nachobrito/sdd/ecommerce/application/service/OrderService.java`
- [X] T012 [US1] Deprecate/remove obsolete synchronous `OrderCreatedNotificationListener` in `src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/messaging/OrderCreatedNotificationListener.java`

**Checkpoint**: At this point, User Story 1 (MVP) is fully functional and testable independently

---

## Phase 4: User Story 2 - Asynchronous Notification for Updated Orders (Priority: P2)

**Goal**: Deliver asynchronous notification dispatch when an existing order's status is updated.

**Independent Test**: Can be tested independently by updating an order's status via `updateOrderStatus`, verifying `OrderUpdatedEvent` published to RabbitMQ, and confirming notification dispatch with update subject.

- [X] T013 [P] [US2] Create domain event `OrderUpdatedEvent` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/event/OrderUpdatedEvent.java`
- [X] T014 [P] [US2] Define inbound driving use case `UpdateOrderStatusUseCase` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/UpdateOrderStatusUseCase.java`
- [X] T015 [US2] Add `updateStatus(OrderStatus newStatus)` method to `Order` aggregate with domain validations in `src/main/java/es/nachobrito/sdd/ecommerce/domain/model/Order.java` and unit test in `src/test/java/es/nachobrito/sdd/ecommerce/domain/OrderTest.java`
- [X] T016 [US2] Implement `UpdateOrderStatusUseCase` in `OrderService` publishing `OrderUpdatedEvent` in `src/main/java/es/nachobrito/sdd/ecommerce/application/service/OrderService.java`
- [X] T017 [US2] Add unit test for `OrderService.updateOrderStatus` in `src/test/java/es/nachobrito/sdd/ecommerce/application/service/OrderServiceTest.java`

**Checkpoint**: At this point, User Stories 1 AND 2 both work independently

---

## Phase 5: User Story 3 - Asynchronous Notification for Cancelled Orders (Priority: P3)

**Goal**: Deliver asynchronous notification dispatch when an order is cancelled.

**Independent Test**: Can be tested independently by cancelling an order via `cancelOrder`, verifying `OrderCancelledEvent` published to RabbitMQ, and confirming notification dispatch with cancellation subject.

- [X] T018 [P] [US3] Create domain event `OrderCancelledEvent` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/event/OrderCancelledEvent.java`
- [X] T019 [P] [US3] Define inbound driving use case `CancelOrderUseCase` in `src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/CancelOrderUseCase.java`
- [X] T020 [US3] Add `cancel(String reason)` method to `Order` aggregate in `src/main/java/es/nachobrito/sdd/ecommerce/domain/model/Order.java` and unit test in `src/test/java/es/nachobrito/sdd/ecommerce/domain/OrderTest.java`
- [X] T021 [US3] Implement `CancelOrderUseCase` in `OrderService` publishing `OrderCancelledEvent` in `src/main/java/es/nachobrito/sdd/ecommerce/application/service/OrderService.java`
- [X] T022 [US3] Add unit test for `OrderService.cancelOrder` in `src/test/java/es/nachobrito/sdd/ecommerce/application/service/OrderServiceTest.java`

**Checkpoint**: All three order lifecycle notification stories are independently functional

---

## Phase 6: User Story 4 - Ephemeral Integration Testing via Test Resources (Priority: P4)

**Goal**: Provide comprehensive integration test coverage using ephemeral RabbitMQ container automatically provisioned via Micronaut TestResources.

**Independent Test**: Run `./gradlew test --tests "es.nachobrito.sdd.ecommerce.infrastructure.OrderNotificationRabbitMqIntegrationTest"` with zero pre-existing broker.

- [X] T023 [US4] Implement end-to-end integration test `OrderNotificationRabbitMqIntegrationTest` using Micronaut TestResources ephemeral RabbitMQ container in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/OrderNotificationRabbitMqIntegrationTest.java`
- [X] T024 [US4] Verify asynchronous delivery and notification dispatch assertions for created, updated, and cancelled orders in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/OrderNotificationRabbitMqIntegrationTest.java`

**Checkpoint**: Full automated integration test suite verified against real ephemeral RabbitMQ broker

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Architectural compliance, regression testing, and code quality verification

- [X] T025 [P] Update ArchUnit rules in `src/test/java/es/nachobrito/sdd/ecommerce/architecture/HexagonalArchitectureTest.java` to verify RabbitMQ adapter boundaries
- [X] T026 [P] Update existing `EventBusIntegrationTest` in `src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/EventBusIntegrationTest.java` to reflect the updated event publisher contract
- [X] T027 Run full quality validation suite `./gradlew check` (Checkstyle, PMD, SpotBugs, ArchUnit, Jacoco >= 80% coverage)
- [X] T028 Validate quickstart verification steps in `specs/005-rabbitmq-order-notifications/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phases 3-6)**: Depend on Foundational phase completion
  - Phase 3 (US1 - MVP): Can proceed immediately after Phase 2
  - Phase 4 (US2): Can proceed after Phase 2, integrates with US1 event publisher
  - Phase 5 (US3): Can proceed after Phase 2, integrates with US1 event publisher
  - Phase 6 (US4): Validates US1, US2, US3 end-to-end via TestResources
- **Polish (Phase 7)**: Depends on all user stories being complete

### Parallel Opportunities

- Within Phase 1: T002 and T003 can execute in parallel
- Within Phase 2: T006 and T007 can execute in parallel
- Within Phase 3: T009 and T010 can execute in parallel
- Within Phase 4: T013 and T014 can execute in parallel
- Within Phase 5: T018 and T019 can execute in parallel
- Within Phase 7: T025 and T026 can execute in parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T008)
3. Complete Phase 3: User Story 1 (T009-T012)
4. **STOP and VALIDATE**: Test User Story 1 independently with unit tests
5. Proceed with US2, US3, US4, and final polish
