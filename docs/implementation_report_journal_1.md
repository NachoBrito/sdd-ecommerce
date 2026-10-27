# Implementation Journal: Order Notifications Feature (`004-order-notifications`)

**Author**: Antigravity Assistant & Engineering Team  
**Feature**: Order Notifications (SMTP Transport MVP + Multi-Transport Architecture)  
**Target Quality Bar**: `./gradlew check` passing with zero warnings, zero lint violations, zero ArchUnit regressions, and zero build configuration tampering.

---

## Developer's Note & Context

In this repository, we operate under a strict non-negotiable rule defined in [AGENTS.md](../AGENTS.md):
> *"When a build check or test fails: Carefully read and analyze the compiler, linter, ArchUnit, or test diagnostic output. Locate the exact root cause in the application, domain, or test source code and fix the code. **NEVER modify project build configuration (`build.gradle`, Checkstyle, PMD, SpotBugs rules, or compiler flags) to bypass an error without explicit user authorization.**"*

This journal chronicles the step-by-step implementation of the feature: what was written initially, how each static validation tool responded, what broke, and how we adapted our code until every quality gate was passed.

---

## Log Entry 1: Laying the Domain Foundation & Inbound/Outbound Ports

### What I Implemented
I started from the core domain inwards, following Hexagonal Architecture:
1. **Value Objects**: [NotificationAddress.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/model/NotificationAddress.java) and [NotificationLineItem.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/model/NotificationLineItem.java) as pure Java records.
2. **Aggregate Model**: [OrderNotification.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/model/OrderNotification.java) with compact constructor invariants (valid email, positive amounts, defensive unmodifiable copies of line items).
3. **Ports**:
   - Driving port: [SendOrderNotificationUseCase.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/port/inbound/SendOrderNotificationUseCase.java)
   - Driven port: [NotificationTransportPort.java](../src/main/java/es/nachobrito/sdd/ecommerce/domain/port/outbound/NotificationTransportPort.java)
4. **Unit Test**: [OrderNotificationTest.java](../src/test/java/es/nachobrito/sdd/ecommerce/domain/model/OrderNotificationTest.java).

### Did it pass static validation?
- **Domain layer**: ✅ Passed. Zero framework dependencies (`import java.*` only). ArchUnit happy.
- **Unit test**: ❌ **Failed PMD**.
  - PMD flagged test fixtures with `LocalVariableCouldBeFinal`.
  - PMD flagged assertions with `UnitTestContainsTooManyAsserts` and `UnitTestAssertionsShouldIncludeMessage`.

### How I Fixed It
1. Added `final` to all local variables in `OrderNotificationTest`.
2. Annotated the test class with `@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals", "PMD.UnitTestAssertionsShouldIncludeMessage"})` matching existing repository conventions established in `OrderTest.java`.

---

## Log Entry 2: Initial Application & Adapter Implementation

### What I Implemented
To implement User Story 1 (SMTP delivery) and User Story 2 (Multi-transport dispatching):
1. **Configuration**: [SmtpConfiguration.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpConfiguration.java) with `@ConfigurationProperties("smtp")`.
2. **Template**: [order-confirmation.html](../src/main/resources/templates/order-confirmation.html) placed in `src/main/resources/templates/`.
3. **Template Renderer**: [SmtpHtmlTemplateRenderer.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpHtmlTemplateRenderer.java) reading the HTML template and substituting placeholders with HTML-escaped values.
4. **Zero-Dependency Mail Client**: Rather than altering `build.gradle` to add Jakarta Mail, I authored:
   - [SmtpSender.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpSender.java) interface.
   - [SocketSmtpSender.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SocketSmtpSender.java) using standard `java.net.Socket` to communicate with the SMTP server via RFC 5321 commands.
   - [SmtpDeliveryException.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpDeliveryException.java).
5. **Transport Adapter**: [SmtpNotificationTransport.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/output/notification/smtp/SmtpNotificationTransport.java).
6. **Application Service**: [NotificationService.java](../src/main/java/es/nachobrito/sdd/ecommerce/application/service/NotificationService.java) querying `GetOrderUseCase` and looping through all registered `NotificationTransportPort` beans.
7. **Messaging Inbound Adapter**: [OrderCreatedNotificationListener.java](../src/main/java/es/nachobrito/sdd/ecommerce/infrastructure/adapter/input/messaging/OrderCreatedNotificationListener.java) listening to Micronaut's `OrderCreatedEvent`.

---

## Log Entry 3: First Build Verification — Compiler `-Werror` & Checkstyle Failures

### Static Validation Command
```bash
./gradlew check
```

### What Broke?

#### 1. Java Compiler: Fatal Warning on Exception Serialization
```text
> Task :compileJava FAILED
.../SmtpDeliveryException.java:8: warning: [serial] serializable class SmtpDeliveryException has no definition of serialVersionUID
public class SmtpDeliveryException extends RuntimeException {
       ^
error: warnings found and -Werror specified
```
- **Why it failed**: In `build.gradle`, `options.compilerArgs` includes `-Xlint:all` and `-Werror`. Because `RuntimeException` implements `java.io.Serializable`, missing `serialVersionUID` is a compiler lint warning. With `-Werror`, javac converts this warning into a fatal build error.
- **My initial thought**: Could we turn off `-Xlint:all`? **No**, forbidden by Rule 2.
- **The code fix**: Added the serial UID to the exception:
  ```java
  @Serial
  private static final long serialVersionUID = 1L;
  ```

#### 2. Checkstyle: CustomImportOrder Violations
```text
> Task :checkstyleTest FAILED
[ERROR] SmtpHtmlTemplateRendererTest.java:15: Import statement is in the wrong order. Should be in the 'STATIC' group. [CustomImportOrder]
[ERROR] SmtpNotificationTransportTest.java:23: Extra separation in import group before 'java.time.Instant' [CustomImportOrder]
```
- **Why it failed**: Checkstyle in this repository strictly enforces a 3-tier import hierarchy separated by single blank lines:
  1. Static imports (`import static ...`)
  2. Blank line
  3. Third-party and project imports (`es.nachobrito.*`, `org.junit.*`, `org.mockito.*`, `io.micronaut.*`)
  4. Blank line
  5. Standard Java packages (`java.time.*`, `java.util.*`)
- **The code fix**: Reorganized imports in all newly created test files to match the exact 3-group hierarchy.

---

## Log Entry 4: Second Build Verification — The `pmdMain` Code Quality Gauntlet

With compiler and checkstyle passing, the build hit the PMD AST static analysis task for production sources.

### Static Validation Command
```bash
./gradlew pmdMain
```

### What Broke?
PMD rejected several patterns in my initial implementation:

```text
> Task :pmdMain FAILED
SocketSmtpSender.java:31: ImplicitFunctionalInterface: Single abstract method interface SmtpSender should use @FunctionalInterface
SocketSmtpSender.java:49: LocalVariableCouldBeFinal: Local variable 'socket' could be declared final
SocketSmtpSender.java:54: AssignmentInOperand: Assignment inside conditional expression
SocketSmtpSender.java:78: AvoidCatchingGenericException: Avoid catching generic exceptions such as Exception
SmtpHtmlTemplateRenderer.java:45: InsufficientStringBufferDeclaration: Initial buffer capacity was not specified
SmtpHtmlTemplateRenderer.java:51: ConsecutiveAppendsShouldReuse: Consecutive append calls should be chained
```

### Analysis & Fixes

1. **`ImplicitFunctionalInterface` on `SmtpSender`**:
   - *Fix*: Marked `public interface SmtpSender` with `@FunctionalInterface`.

2. **`AssignmentInOperand` in `SocketSmtpSender`**:
   - *What was initially written*:
     ```java
     String line;
     while ((line = reader.readLine()) != null) {
         response.append(line).append("\n");
         if (line.length() >= 4 && line.charAt(3) == ' ') break;
     }
     ```
   - *Why PMD rejected it*: PMD rules disallow assignments within boolean conditional expressions to prevent accidental `=` instead of `==`.
   - *How I fixed it in code*:
     ```java
     String line = reader.readLine();
     while (line != null) {
         response.append(line).append('\n');
         if (line.length() >= 4 && line.charAt(3) == ' ') {
             break;
         }
         line = reader.readLine();
     }
     ```

3. **Numeric Literals without Underscores in `SocketSmtpSender`**:
   - *What was initially written*: `socket.setSoTimeout(10000);`
   - *Why PMD rejected it*: Readability rule requires digit separators for large numeric constants.
   - *How I fixed it*: `socket.setSoTimeout(10_000);`

4. **`InsufficientStringBufferDeclaration` & `ConsecutiveAppendsShouldReuse` in `SmtpHtmlTemplateRenderer`**:
   - *What was initially written*:
     ```java
     StringBuilder sb = new StringBuilder();
     sb.append("<div class=\"line-item\">");
     sb.append("<span>Product: ");
     sb.append(escapeHtml(line.productId().toString()));
     sb.append("</span>");
     ```
   - *Why PMD rejected it*: Default `StringBuilder()` starts with a tiny 16-char capacity, forcing repeated array reallocations. Also, consecutive independent append statements should be chained.
   - *How I fixed it*:
     ```java
     final StringBuilder sb = new StringBuilder(512);
     sb.append("<div class=\"line-item\">")
       .append("<span>Product: ")
       .append(escapeHtml(line.productId().toString()))
       .append("</span>");
     ```

5. **`AvoidCatchingGenericException` in `SmtpNotificationTransport` & `NotificationService`**:
   - *Why PMD flagged it*: Catching `Exception` is flagged as overly broad.
   - *The conflict with requirements*: Architecture Principle 7 (Fault Isolation) requires that notification delivery failures MUST NOT abort or roll back the order creation transaction. If SMTP or any unknown transport throws, it must be caught and logged safely.
   - *How I fixed it*: I caught `SmtpDeliveryException` directly where feasible, and on the public transport boundary method, added `@SuppressWarnings("PMD.AvoidCatchingGenericException")` with clear comments explaining why boundary fault isolation requires catching unexpected exceptions.

After these changes, `./gradlew pmdMain` passed with 0 violations!

---

## Log Entry 5: Third Build Verification — The 74 `pmdTest` Violations

Next, I ran the test verification suite.

### Static Validation Command
```bash
./gradlew pmdTest
```

### What Broke?
A barrage of 74 violations appeared across `SmtpHtmlTemplateRendererTest`, `SmtpNotificationTransportTest`, `NotificationServiceTest`, and `OrderCreatedNotificationListenerTest`:

```text
> Task :pmdTest FAILED
SmtpHtmlTemplateRendererTest.java:77: LocalVariableCouldBeFinal: Local variable 'html' could be declared final
SmtpHtmlTemplateRendererTest.java:79: UnitTestAssertionsShouldIncludeMessage: Unit test assertions should include a message
SmtpHtmlTemplateRendererTest.java:89: UnitTestContainsTooManyAsserts: Unit tests should not contain more than 1 assert(s)
SmtpNotificationTransportTest.java:54: AvoidDuplicateLiterals: The String literal "no-reply@ecommerce.com" appears 4 times in this file
NotificationServiceTest.java:39: LocalVariableCouldBeFinal: Local variable 'delivery' could be declared final
```

### The First Attempt
1. I marked every local test variable as `final` (`final UUID orderId = ...;`, `final String html = ...;`).
2. Looking at `OrderTest.java` in the same project, I saw it used class-level suppressions for unit test specifics. I applied the same suppressions to all four new test classes:
   ```java
   @SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals", "PMD.UnitTestAssertionsShouldIncludeMessage"})
   ```

---

## Log Entry 6: Fourth Build Verification — The "Unnecessary Warning Suppression" Trap

I thought `pmdTest` was going to be completely clean. Instead, it failed again:

### Static Validation Command
```bash
./gradlew pmdTest
```

### What Broke?
```text
> Task :pmdTest FAILED
/es/nachobrito/.../NotificationServiceTest.java:28:
UnnecessaryWarningSuppression: Unnecessary suppression "PMD.UnitTestAssertionsShouldIncludeMessage" in @SuppressWarnings annotation

/es/nachobrito/.../OrderCreatedNotificationListenerTest.java:14:
UnnecessaryWarningSuppression: Unnecessary @SuppressWarnings annotation
```

### Why Did This Happen?
PMD has `UnnecessaryWarningSuppression` enabled in `config/pmd/pmd.xml`.
- In `NotificationServiceTest`, all assertions are Mockito `verify(...)` and JUnit `assertDoesNotThrow(...)`. There are **no** `assertEquals` or `assertTrue` calls that omit failure messages! Therefore, suppressing `UnitTestAssertionsShouldIncludeMessage` was redundant, and PMD flagged the suppression itself as dead code.
- In `OrderCreatedNotificationListenerTest`, there is only one test method, with a single `verify(...)`, and no duplicate string literals. Suppressing the three rules was 100% unnecessary!

### The Surgical Fix
I audited each test file individually:
- **`SmtpHtmlTemplateRendererTest`**: Kept all 3 suppressions (it has multiple asserts, assertions without messages, and duplicate literals).
- **`SmtpNotificationTransportTest`**: Kept all 3 suppressions (uses multiple asserts and duplicate email literals).
- **`NotificationServiceTest`**: Removed `UnitTestAssertionsShouldIncludeMessage`, leaving only:
  ```java
  @SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
  ```
- **`OrderCreatedNotificationListenerTest`**: Completely removed `@SuppressWarnings`.

Re-ran `./gradlew pmdTest`:
```text
BUILD SUCCESSFUL in 4s
```

---

## Log Entry 7: Final End-to-End Build Verification

With all specific linters satisfied, I executed the full verification pipeline to validate compiler, linters, ArchUnit, Testcontainers, and code coverage together.

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
> Task :compileTestJava
> Task :checkstyleTest UP-TO-DATE
> Task :pmdTest UP-TO-DATE
> Task :spotbugsTest

> Task :test
NotificationService Application Service Tests > Should continue to next transport if one transport throws an unhandled exception PASSED
NotificationService Application Service Tests > Should skip notification when order is not found PASSED
NotificationService Application Service Tests > Should skip disabled transports while executing enabled ones PASSED
NotificationService Application Service Tests > Should handle empty transport list gracefully without throwing PASSED
NotificationService Application Service Tests > Should dispatch notification to all enabled transports PASSED
HexagonalArchitectureTest > Inbound ports must only reside in domain.port.inbound PASSED
HexagonalArchitectureTest > Domain layer must not depend on Application or Infrastructure layers PASSED
HexagonalArchitectureTest > Outbound ports must only reside in domain.port.outbound PASSED
HexagonalArchitectureTest > Application layer must not depend on Infrastructure adapters PASSED
HexagonalArchitectureTest > Domain layer must be pure Java with zero framework imports PASSED
OrderCreatedNotificationListener Unit Tests > Should forward OrderCreatedEvent orderId and traceId to SendOrderNotificationUseCase PASSED
SmtpHtmlTemplateRenderer Unit Tests > Should handle null complement cleanly without printing 'null' PASSED
SmtpHtmlTemplateRenderer Unit Tests > Should successfully render HTML template with all order fields replaced PASSED
SmtpHtmlTemplateRenderer Unit Tests > Should escape HTML characters to prevent XSS injection PASSED
SmtpNotificationTransport Unit Tests > Should catch transmission exceptions without rethrowing (fault isolation) PASSED
SmtpNotificationTransport Unit Tests > Should skip delivery when transport is disabled PASSED
SmtpNotificationTransport Unit Tests > Should deliver notification when transport is enabled PASSED
[test-resources-service] INFO tc.mariadb:latest - Container is started (JDBC URL: jdbc:mariadb://localhost:32869/test)
OrderControllerIntegrationTest > POST /v1/orders creates order with valid details and returns 201 Created PASSED
... (35 total tests passed)

> Task :jacocoTestCoverageVerification
> Task :jacocoTestReport
> Task :check

BUILD SUCCESSFUL in 26s
15 actionable tasks: 6 executed, 9 up-to-date
```

---

## Key Lessons from the Journal

1. **Build Config Rules Must Not Be Weakened**:
   When `AssignmentInOperand` or `serialVersionUID` errors occurred, the instinct of a hurried developer might be to disable `-Werror` or suppress the rule in `config/pmd/pmd.xml`. Following Rule 2 forced us to write safer Java code: an explicit serial version UID, a safer loop construct, and explicit string buffer allocations.

2. **Over-Suppressing Is a Code Smell**:
   Copy-pasting `@SuppressWarnings` across all test files backfired because PMD detected that the suppressed issues did not exist in those files. Suppressions must be applied surgically and only where genuinely necessary.

3. **Zero External Dependencies via Native Sockets**:
   Rather than modifying `build.gradle` to add heavy third-party email dependencies, authoring a lightweight RFC 5321 socket client satisfied all business requirements, complied with architectural isolation, and kept the project build configuration intact.
