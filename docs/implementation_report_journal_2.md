# Implementation Journal: Asynchronous Order Notifications via RabbitMQ (`005-rabbitmq-order-notifications`)

**Author**: Antigravity Assistant & Engineering Team  
**Feature**: Asynchronous Order Notifications via RabbitMQ (Topic Exchange, Ephemeral TestContainers, Inbound/Outbound Ports)  
**Target Quality Bar**: `./gradlew check` passing with zero warnings, zero lint violations, zero ArchUnit regressions, and zero build configuration tampering.

---

## Developer's Note & Context

In this repository, we operate under a strict non-negotiable rule defined in [AGENTS.md](../AGENTS.md):
> *"When a build check or test fails: Carefully read and analyze the compiler, linter, ArchUnit, or test diagnostic output. Locate the exact root cause in the application, domain, or test source code and fix the code. **NEVER modify project build configuration (`build.gradle`, Checkstyle, PMD, SpotBugs rules, or compiler flags) to bypass an error without explicit user authorization.**"*

This journal chronicles the end-to-end implementation of the asynchronous notification system via RabbitMQ: migrating away from synchronous in-memory Micronaut events, configuring declarative broker topologies, navigating subtle container networking and test transaction concurrency gotchas, and satisfying our strict static analysis tools.

---

## Log Entry 1: Laying the Domain & Inbound/Outbound Ports for Order Lifecycle

### What I Implemented
The feature requested publishing RabbitMQ events not only when an order is created, but also when an order is updated or cancelled:

1. **Domain Events**:
   - Authored [OrderUpdatedEvent.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/event/OrderUpdatedEvent.java) and [OrderCancelledEvent.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/event/OrderCancelledEvent.java) as pure Java records with compact constructors and static factory methods (`.create(...)`).
2. **Outbound Port Extension**:
   - Updated [EventPublisherPort.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/port/outbound/EventPublisherPort.java) to declare `publish(OrderUpdatedEvent)` and `publish(OrderCancelledEvent)`.
3. **Driving Use Case Ports**:
   - Authored [UpdateOrderStatusUseCase.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/UpdateOrderStatusUseCase.java) and [CancelOrderUseCase.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/CancelOrderUseCase.java).
4. **Aggregate Encapsulation on `Order`**:
   - Added `updateStatus(OrderStatus newStatus)` and `cancel()` to [Order.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/model/Order.java).
   - Invariant enforcement: an order that is already in `OrderStatus.CANCELLED` throws `InvalidOrderException` if an attempt is made to update its status.
5. **Application Service Wiring**:
   - Implemented the use cases in [OrderService.java](../src/main/java/es/nachobrito/sdd/ecommerce/application/service/OrderService.java), persisting state changes through `OrderRepositoryPort` and publishing the respective domain events through `EventPublisherPort`.

---

## Log Entry 2: RabbitMQ Topology, Wire DTO, Producer, and Consumer

### What I Implemented
With pure business logic established, I moved to the framework boundary in `infrastructure`:

1. **Boundary Wire DTO**:
   - Authored [OrderEventMessage.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/dto/OrderEventMessage.java) marked with `@Serdeable.Serializable` and `@Serdeable.Deserializable` to represent the wire contract payload over AMQP.
2. **Declarative RabbitMQ Client**:
   - Created [RabbitOrderEventClient.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/RabbitOrderEventClient.java) using `@RabbitClient("ecommerce.orders.exchange")`:
     - `@Binding("order.created") void sendOrderCreated(OrderEventMessage message)`
     - `@Binding("order.updated") void sendOrderUpdated(OrderEventMessage message)`
     - `@Binding("order.cancelled") void sendOrderCancelled(OrderEventMessage message)`
3. **Topology Initializer**:
   - Authored [RabbitMqTopologyInitializer.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/RabbitMqTopologyInitializer.java) implementing Micronaut's `ChannelInitializer`.
   - Declaratively provisions the topic exchange `ecommerce.orders.exchange`, durable queue `ecommerce.orders.notifications.queue`, and binds them with routing pattern `order.*`.
4. **Outbound Messaging Adapter**:
   - Updated [EventBusAdapter.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/messaging/EventBusAdapter.java) implementing `EventPublisherPort`. It maps domain events to `OrderEventMessage`, dispatches them to `RabbitOrderEventClient`, and also notifies the internal application event publisher.
5. **Asynchronous RabbitMQ Consumer**:
   - Authored [OrderRabbitNotificationConsumer.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/messaging/OrderRabbitNotificationConsumer.java) with `@RabbitListener` bound to `ecommerce.orders.notifications.queue`.
   - On receipt of a message, it extracts `orderId` and `traceId` and asynchronously calls `SendOrderNotificationUseCase.sendOrderNotification(orderId, traceId)`.
6. **Decommissioning the Synchronous Listener**:
   - Removed `@Singleton` and `@EventListener` from [OrderCreatedNotificationListener.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/messaging/OrderCreatedNotificationListener.java) and marked it `@Deprecated(forRemoval = true)` to avoid duplicate processing.
7. **Dynamic HTML Email Templates**:
   - Parameterized [order-confirmation.html](../src/main/resources/templates/order-confirmation.html) with `{{statusTitle}}`, `{{statusSubtitle}}`, and `{{orderStatus}}`.
   - Updated [SmtpHtmlTemplateRenderer.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpHtmlTemplateRenderer.java) and [SmtpNotificationTransport.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpNotificationTransport.java) to dynamically adapt email subject lines and body copy based on status (`Order Confirmation - [id]`, `Order Updated - [id]`, `Order Cancelled - [id]`).

---

## Log Entry 3: First Build Verification — Compiler Warnings as Errors & Test Missing Symbol

### Static Validation Command
```bash
./gradlew test --tests "es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.messaging.OrderCreatedNotificationListenerTest"
```

### What Broke?
```text
> Task :compileTestJava FAILED
.../OrderCreatedNotificationListenerTest.java:18: error: [removal] OrderCreatedNotificationListener in es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.messaging has been deprecated and marked for removal
[Werror] warnings found and -Werror specified
```

### Analysis & Fix
- **Why it failed**: In `build.gradle`, `options.compilerArgs` enforces `-Werror` and `-Xlint:all`. Since `OrderCreatedNotificationListener` was deprecated with `forRemoval = true`, referencing it in its unit test emitted a compiler warning, which javac elevated to a fatal build error.
- **The fix**: Annotated `OrderCreatedNotificationListenerTest` with `@SuppressWarnings({"deprecation", "removal"})`.
- Similarly, in `OrderTest.java`, a helper method `createValidOrder()` was added to streamline test order creation without duplicate fixture code.

---

## Log Entry 4: The RabbitMQ TestResources Connection Trap

### The Test
With the unit tests compiling cleanly, I ran the end-to-end integration test [OrderNotificationRabbitMqIntegrationTest.java](../src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/OrderNotificationRabbitMqIntegrationTest.java) to verify real messaging via Micronaut TestResources.

### What Broke?
```text
> Task :test
OrderNotificationRabbitMqIntegrationTest > Should publish OrderCreatedEvent to RabbitMQ and dispatch async notification FAILED
    io.micronaut.rabbitmq.exception.RabbitClientException
        Caused by: io.micronaut.rabbitmq.connect.recovery.TemporarilyDownIOException
            Caused by: java.net.ConnectException: Connection refused
```

### Investigation
1. The test execution logs clearly confirmed that Micronaut TestResources started the Docker container:
   ```text
   [test-resources-service] INFO tc.rabbitmq:latest - Container rabbitmq:latest started in PT2.423865604S
   ```
2. Yet the client threw `java.net.ConnectException: Connection refused`!
3. I used `javap` and class inspection to decompile `RabbitMQTestResourceProvider.class` in `micronaut-test-resources-rabbitmq`. The provider registers:
   - `rabbitmq.uri` (resolved via `container.getAmqpUrl()`)
   - `rabbitmq.username` (resolved via `container.getAdminUsername()`)
   - `rabbitmq.password` (resolved via `container.getAdminPassword()`)
4. Next, I inspected `RabbitConnectionFactory.java` and `RabbitConnectionFactoryConfig.java` in `micronaut-rabbitmq`. I spotted the critical connection logic:
   ```java
   Optional<List<Address>> addresses = factory.getAddresses();
   if (addresses.isPresent()) {
       return factory.newConnection(executorService, addresses.get());
   }
   return factory.newConnection(executorService);
   ```
5. **The Culprit Found**: In [application.yml](../src/main/resources/application.yml), we had originally defined:
   ```yaml
   rabbitmq:
     addresses:
       - localhost:5672
   ```
   Because `addresses` was populated, `RabbitConnectionFactory` attempted to connect directly to the static `localhost:5672`, completely bypassing the ephemeral dynamic port injected into `rabbitmq.uri` by TestResources!

### The Code Fix
In [application.yml](../src/main/resources/application.yml), removed the static `addresses` block so that `rabbitmq.uri` is used:
```yaml
rabbitmq:
  exchange:
    orders: ${RABBITMQ_ORDERS_EXCHANGE:ecommerce.orders.exchange}
  queue:
    notifications: ${RABBITMQ_ORDERS_NOTIFICATIONS_QUEUE:ecommerce.orders.notifications.queue}
```
Now, TestResources supplies the ephemeral URI dynamically during tests, while local dev or production can provide `RABBITMQ_URI` via environment variables.

---

## Log Entry 5: The Test Transaction Isolation / Repeatable-Read Mystery

With the connection refused error resolved, I re-ran the integration test.

### What Broke?
The connection succeeded, messages were published and consumed, but the test timed out:
```text
OrderNotificationRabbitMqIntegrationTest > Should publish OrderCreatedEvent to RabbitMQ and dispatch async notification FAILED
    java.lang.AssertionError: Condition was not met within 10000 milliseconds
```

### Investigation
I examined the JSON structured logs in the test report:
```json
{"message":"Publishing OrderCreatedEvent to RabbitMQ: eventId=..., orderId=3578f399-e30e-4229-b8b6-d8be5ad9cc2c","logger":"EventBusAdapter","thread":"Test worker"}
{"message":"Received order event message from RabbitMQ: ... orderId=3578f399-e30e-4229-b8b6-d8be5ad9cc2c","logger":"OrderRabbitNotificationConsumer","thread":"pool-2-thread-2"}
{"message":"Executing Query: SELECT ... FROM `orders` WHERE (`id` = ?)","logger":"io.micronaut.data.query","thread":"pool-2-thread-2"}
{"message":"Order not found for notification dispatch: orderId=3578f399-e30e-4229-b8b6-d8be5ad9cc2c","logger":"NotificationService","thread":"pool-2-thread-2","level":"WARN"}
```

Why did `NotificationService.getOrder(...)` return empty when `createOrderUseCase.createOrder(...)` had just executed on the line right before it?

### Root Cause Analysis
1. In `@MicronautTest`, the `transactional` property defaults to `true`.
2. This activates `DefaultTestTransactionExecutionListener`, which automatically wraps every test method execution in a database transaction and rolls it back at the end of the method.
3. Because the test method was running on the `"Test worker"` thread, the SQL `INSERT INTO orders ...` was executed inside an **uncommitted transaction**.
4. The RabbitMQ broker immediately delivered the published message to `OrderRabbitNotificationConsumer` running on an asynchronous worker thread (`"pool-2-thread-2"`).
5. Thread `"pool-2-thread-2"` acquired its own database connection to execute `SELECT ... FROM orders`. Under MariaDB's transaction isolation, uncommitted writes from another transaction are invisible!
6. Thus, `getOrder(orderId)` returned `Optional.empty()`, and the notification was skipped.

### The Code Fix
Configured `@MicronautTest` with `transactional = false` in [OrderNotificationRabbitMqIntegrationTest.java](../src/test/java/es/nachobrito/sdd/ecommerce/infrastructure/OrderNotificationRabbitMqIntegrationTest.java):
```java
@MicronautTest(environments = "test", transactional = false)
class OrderNotificationRabbitMqIntegrationTest {
```
Without the artificial test transaction wrapper, `createOrderUseCase.createOrder(...)` commits immediately upon method return, making the order instantly visible to the concurrent RabbitMQ consumer thread.

Re-ran the test:
```text
OrderNotificationRabbitMqIntegrationTest > Should publish OrderCancelledEvent to RabbitMQ and dispatch async notification PASSED
OrderNotificationRabbitMqIntegrationTest > Should publish OrderCreatedEvent to RabbitMQ and dispatch async notification PASSED
OrderNotificationRabbitMqIntegrationTest > Should publish OrderUpdatedEvent to RabbitMQ and dispatch async notification PASSED
BUILD SUCCESSFUL in 21s
```

---

## Log Entry 6: ArchUnit Architectural Compliance & Framework Isolation

To prevent architectural erosion, task T025 called for verifying RabbitMQ adapter boundaries.

### What I Implemented
In [HexagonalArchitectureTest.java](../src/test/java/es/nachobrito/sdd/ecommerce/architecture/HexagonalArchitectureTest.java), I added a rule enforcing that RabbitMQ framework classes (`io.micronaut.rabbitmq..`, `com.rabbitmq..`) are strictly confined to the `infrastructure` package:

```java
@Test
@DisplayName("RabbitMQ framework dependencies must only reside in infrastructure layer")
void rabbitMqDependenciesMustOnlyResideInInfrastructure() {
    final ArchRule rule = noClasses()
            .that()
            .resideOutsideOfPackage("..infrastructure..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.micronaut.rabbitmq..", "com.rabbitmq..");

    rule.check(importedClasses);
}
```

This test passed instantly, proving that neither `domain` nor `application` contains any leakage of messaging framework imports.

---

## Log Entry 7: The Static Analysis Gauntlet (PMD & SpotBugs)

Next, I executed `./gradlew check` to run our static analysis toolchain (Checkstyle, PMD, SpotBugs, ArchUnit, JaCoCo).

### What Broke?

#### 1. `pmdMain` Violations (6 issues)
```text
OrderService.java:64: AvoidDuplicateLiterals: The String literal "Order id must not be null" appears 4 times in this file
EventBusAdapter.java:45: AvoidDuplicateLiterals: The String literal "event must not be null" appears 4 times in this file
SmtpHtmlTemplateRenderer.java:40: AvoidLiteralsInIfCondition: Avoid using literals such as "CANCELLED" in if statements
SmtpHtmlTemplateRenderer.java:44: AvoidLiteralsInIfCondition: Avoid using literals such as "CREATED" in if statements
SmtpNotificationTransport.java:59: AvoidLiteralsInIfCondition: Avoid using literals such as "CANCELLED" in if statements
SmtpNotificationTransport.java:61: AvoidLiteralsInIfCondition: Avoid using literals such as "CREATED" in if statements
```

**Fixes**:
- Defined constant `ORDER_ID_MUST_NOT_BE_NULL` in `OrderService.java`.
- Defined constant `EVENT_MUST_NOT_BE_NULL` in `EventBusAdapter.java`.
- Defined constants `STATUS_CANCELLED = "CANCELLED"` and `STATUS_CREATED = "CREATED"` in both `SmtpHtmlTemplateRenderer.java` and `SmtpNotificationTransport.java`.
- `./gradlew pmdMain` passed with 0 violations!

#### 2. `spotbugsTest` Violation (1 issue)
```text
BugInstance: RV_RETURN_VALUE_IGNORED_INFERRED
Return value of Order.updateStatus(OrderStatus) ignored in OrderTest.lambda$shouldRejectStatusUpdateOnCancelledOrder$0(Order)
```
**Fix**:
In `OrderTest.java`:
```java
assertThrows(InvalidOrderException.class, () -> {
    final Order ignored = cancelled.updateStatus(OrderStatus.SHIPPED);
    assertNotNull(ignored, "Should never be reached due to exception");
}, "Updating status of a cancelled order must fail");
```
- `./gradlew spotbugsTest` passed!

#### 3. `pmdTest` Violations (53 issues)
- `UnitTestAssertionsShouldIncludeMessage`: Every JUnit assertion must have an informative error description message.
- `UnitTestContainsTooManyAsserts`: Tests with multiple assertions must wrap them inside `assertAll("description", ...)` or declare suppression.
- `DoNotUseThreads`: Flagged `Thread.sleep(100)` inside the `waitUntil` helper in `OrderNotificationRabbitMqIntegrationTest`.

**Fixes**:
- Added descriptive messages to all `assertEquals`, `assertNotNull`, and `assertThrows` across `EventBusIntegrationTest`, `OrderServiceTest`, `OrderNotificationRabbitMqIntegrationTest`, and `EventBusAdapterTest`.
- Grouped related assertions into `assertAll(...)` blocks.
- Added `@SuppressWarnings("PMD.DoNotUseThreads")` to `OrderNotificationRabbitMqIntegrationTest` to permit test polling.
- Removed redundant `@SuppressWarnings("PMD.AvoidDuplicateLiterals")` where dead code suppression was detected.
- `./gradlew pmdTest` passed with 0 violations!

---

## Log Entry 8: Final End-to-End Build Verification

With all code fixes applied, I executed the full verification pipeline:

### Static Validation Command
```bash
./gradlew check
```

### Verification Output
```text
> Task :compileJava UP-TO-DATE
> Task :checkstyleMain UP-TO-DATE
> Task :pmdMain UP-TO-DATE
> Task :spotbugsMain UP-TO-DATE
> Task :compileTestJava UP-TO-DATE
> Task :checkstyleTest UP-TO-DATE
> Task :pmdTest UP-TO-DATE
> Task :spotbugsTest

> Task :test
OrderNotificationRabbitMqIntegrationTest > Should publish OrderCancelledEvent to RabbitMQ and dispatch async notification PASSED
OrderNotificationRabbitMqIntegrationTest > Should publish OrderCreatedEvent to RabbitMQ and dispatch async notification PASSED
OrderNotificationRabbitMqIntegrationTest > Should publish OrderUpdatedEvent to RabbitMQ and dispatch async notification PASSED
EventBusIntegrationTest > Publishing ProductCreatedEvent notifies internal event listeners PASSED
EventBusIntegrationTest > Publishing OrderCreatedEvent notifies internal event listeners and RabbitMQ PASSED
EventBusIntegrationTest > Publishing OrderCancelledEvent notifies internal event listeners and RabbitMQ PASSED
EventBusIntegrationTest > Publishing OrderUpdatedEvent notifies internal event listeners and RabbitMQ PASSED
HexagonalArchitectureTest > Inbound ports must only reside in domain.port.inbound PASSED
HexagonalArchitectureTest > Domain layer must not depend on Application or Infrastructure layers PASSED
HexagonalArchitectureTest > Outbound ports must only reside in domain.port.outbound PASSED
HexagonalArchitectureTest > Application layer must not depend on Infrastructure adapters PASSED
HexagonalArchitectureTest > RabbitMQ framework dependencies must only reside in infrastructure layer PASSED
HexagonalArchitectureTest > Domain layer must be pure Java with zero framework imports PASSED
OrderControllerIntegrationTest > POST /v1/orders creates order with valid details and returns 201 Created PASSED
... (34 total tests passed)

> Task :jacocoTestCoverageVerification
> Task :jacocoTestReport
> Task :check

BUILD SUCCESSFUL in 38s
15 actionable tasks: 8 executed, 7 up-to-date
```

---

## Key Lessons from the Journal

1. **Beware of Static Configuration Defaults with TestContainers**:
   Hardcoding `addresses: [localhost:5672]` in `application.yml` silently overrode Micronaut TestResources' dynamic AMQP URI resolver. Configuration properties in `application.yml` should always prefer environment variable placeholders (`${RABBITMQ_URI:...}`) to allow test containers to inject ephemeral ports dynamically.

2. **Async Integration Tests Must Account for Transaction Boundaries**:
   Default test transaction rollback mechanisms (`transactional = true`) are incompatible with asynchronous message brokers when the consumer must read state from the database. Because consumer threads run in separate database sessions, data must be committed from the test thread to be visible.

3. **Zero Tampering Principle Enforces Higher Code Quality**:
   When SpotBugs warned about an ignored return value (`RV_RETURN_VALUE_IGNORED_INFERRED`) and PMD warned about uninformative assertion messages, the temptation might be to weaken the linter rules. By refusing to modify build configurations, we improved test diagnostics and made assertion failures far easier to troubleshoot in CI.

4. **Hexagonal Architecture Keeps Message Broker Migrations Clean**:
   Because `NotificationService` and `Order` depend exclusively on ports (`SendOrderNotificationUseCase`, `EventPublisherPort`), swapping from synchronous event publishing to asynchronous RabbitMQ topic routing was achieved entirely within `infrastructure` without changing a single line of business logic.
