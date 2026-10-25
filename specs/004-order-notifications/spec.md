# Feature Specification: Order Notifications

**Feature Branch**: `004-order-notifications`  
**Created**: 2026-10-25  
**Status**: Draft  
**Input**: User description: "Define a new feature for the e-commerce system: notifications. 1. Create a notification service that accepts multiple transports, SMTP initially. 2. When an order is created, the notification service sends the customer the order details. 3. Each transport uses its own template. The SMTP transport will have an html template."

## Clarifications

### Session 2026-10-25

- Q: How does the notification service receive notification triggers? → A: In-process domain event listening via `OrderCreatedEvent` published through Micronaut's application event bus, ensuring decoupling and zero external queue dependencies (Constitution Principle 1 & 6).
- Q: How are order details obtained for the notification? → A: The notification service queries the complete `Order` aggregate by ID via `GetOrderUseCase` upon receiving `OrderCreatedEvent`, ensuring the notification payload contains complete customer and line item details.
- Q: What happens if an external notification transport (e.g. SMTP) fails or times out? → A: The exception is caught and logged as an error with trace context; the failure is isolated and does not roll back or fail the order creation transaction (Constitution Principle 7: Recovery Over Prevention).
- Q: What sender address and subject line format are used for SMTP notifications? → A: The sender address is configurable via application properties (defaulting to `no-reply@ecommerce.example.com`), and the subject line is formatted as `Order Confirmation - [orderId]`.
- Q: How are multiple transports orchestrated? → A: The notification service maintains a collection of registered `NotificationTransport` beans. When an order notification is triggered, it invokes each active transport's delivery method.
- Q: How are HTML templates managed and rendered for SMTP? → A: The SMTP transport uses a dedicated template renderer loading an HTML template from classpath resources (`templates/order-confirmation.html`), safely interpolating order attributes with HTML escaping.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Send Order Confirmation via SMTP HTML Email (Priority: P1) 🎯 MVP

As an e-commerce customer placing an order, I want to receive an email notification formatted in clean HTML immediately after my order is created, so that I have an immediate confirmation and an accessible record of my purchased items, billing and delivery addresses, and total payment.

**Why this priority**: Core value of the feature. Sending confirmation email upon order placement directly reassures the buyer and is the primary customer-facing deliverable.

**Independent Test**: Can be tested independently by simulating/triggering an order creation event and verifying that the notification service invokes the SMTP transport with the customer's email address, a subject line with the order ID, and an HTML body populated with the order details.

**Acceptance Scenarios**:

1. **Given** an order has been created with customer email `customer@example.com`, customer name `Jane Doe`, order ID `123e4567-e89b-12d3-a456-426614174000`, order date, delivery address, invoice address, order lines, and total amount, **When** the order creation event is received by the notification service, **Then** the notification service dispatches an email via the SMTP transport to `customer@example.com` containing the rendered HTML template with all order details.
2. **Given** an order with multiple order lines, **When** the SMTP HTML template is rendered, **Then** each item's product ID, quantity, unit price, and line total are displayed alongside the grand total and currency.
3. **Given** an order creation event with an invalid or unresolvable order ID, **When** the notification service attempts to fetch the order details, **Then** the service logs the error and does not send a notification.

---

### User Story 2 - Multiple Transports Architecture (Priority: P2)

As a platform developer or operations engineer, I want the notification service to support a pluggable transport registry that can accept multiple transports (with SMTP as the initial built-in transport), so that future notification channels (e.g., SMS, push notifications, webhooks) can be added without modifying the core notification dispatching logic.

**Why this priority**: Architectural foundation required by requirement #1 ("accepts multiple transports, SMTP initially"). Ensures decoupling between notification use-cases and specific delivery mechanisms.

**Independent Test**: Can be tested independently by registering multiple mock/stub transports with the notification service, triggering a notification dispatch, and verifying that all registered active transports receive the notification payload.

**Acceptance Scenarios**:

1. **Given** a notification service configured with multiple transports, **When** a notification dispatch request is executed, **Then** the notification service routes the notification to each active registered transport.
2. **Given** only the SMTP transport is configured/enabled, **When** an order notification is dispatched, **Then** only the SMTP transport executes its send operation.
3. **Given** no transports are registered or active, **When** an order notification is dispatched, **Then** the service logs a warning and completes without throwing an unhandled exception.

---

### User Story 3 - Transport-Specific Template Management (Priority: P3)

As a notification content designer, I want each notification transport to manage and render its own medium-specific template (specifically an HTML template for the SMTP transport), so that content is tailored and optimized for the constraints and formatting capabilities of each transport medium.

**Why this priority**: Required by requirement #3 ("Each transport uses its own template. The SMTP transport will have an html template"). Decouples message representation from delivery channels.

**Independent Test**: Can be tested independently by rendering the template for the SMTP transport with sample order data and verifying the output is valid, well-formed HTML containing all required placeholders replaced by order values.

**Acceptance Scenarios**:

1. **Given** an order data model, **When** the SMTP transport template engine renders the email content, **Then** it produces an HTML document containing order ID, customer name, date, itemized table of order lines, total amount formatted with currency, delivery address, and invoice address.
2. **Given** a new or alternative transport is added in the future, **When** that transport processes an order notification, **Then** it utilizes its own independent template without affecting or depending on the SMTP HTML template.

---

### Edge Cases

- **SMTP Server Unreachable / Timeout**: If the external SMTP server is down or returns a connection error, the SMTP transport MUST catch the exception, log the failure with error details and trace ID, and NOT cause the already-persisted order transaction to fail or roll back.
- **Missing or Invalid Customer Email**: If an order has an empty or malformed customer email, the SMTP transport MUST record a validation error and skip transmission without crashing the application.
- **Special Characters in Order Content**: HTML templates MUST escape dynamic text content (such as customer names or street addresses) to prevent HTML injection.
- **Empty Order Lines**: The template rendering MUST handle orders gracefully, though by domain rules orders always contain at least one order line.
- **Optional Complement in Addresses**: When the address `complement` field is null or omitted, the HTML template MUST omit or format the address cleanly without displaying "null".

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a notification service that dispatches order notifications when an order is created.
- **FR-002**: The notification service MUST support multiple notification transports through an extensible transport contract (with SMTP provided as the initial built-in transport).
- **FR-003**: The notification service MUST listen for or consume `OrderCreatedEvent` domain events published upon order creation.
- **FR-004**: Upon receiving an order creation event, the notification service MUST obtain the complete order details (customer name, customer email, order date, status, line items, amounts, currency, delivery address, invoice address).
- **FR-005**: The SMTP transport MUST render an HTML email template populated with the order details.
- **FR-006**: The SMTP HTML template MUST include: order ID, creation date, customer name, delivery address, invoice address, an itemized list of order lines (product ID, quantity, unit price in cents, line total in cents), total amount in cents, and currency.
- **FR-007**: The SMTP transport MUST deliver the generated HTML email to the customer's email address specified on the order.
- **FR-008**: Each transport MUST manage its own transport-specific template and rendering mechanism independently.
- **FR-009**: Notification transmission failures in any transport MUST be logged with diagnostic context and MUST NOT roll back or cancel the created order.
- **FR-010**: The domain and application logic of the notification service MUST remain framework-agnostic, adhering to Hexagonal Architecture boundaries.

### Key Entities *(include if feature involves data)*

- **NotificationTransport**: Outbound port interface representing a transport medium (e.g. SMTP) capable of rendering its template and delivering an order notification.
- **OrderNotification**: Value object representing the notification payload containing order summary details, recipient information, and metadata.
- **NotificationTemplate**: Abstraction representing a transport-specific template responsible for rendering notification content for a given transport format (e.g. HTML for SMTP).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of successfully created orders trigger an order notification dispatch to all active transports.
- **SC-002**: The SMTP transport successfully renders a valid HTML email body containing all order details within 50 milliseconds of template processing time.
- **SC-003**: If the SMTP transport fails during transmission, the order creation endpoint still completes successfully with HTTP 201 Created and zero order rollbacks.
- **SC-004**: Adding a new transport in the future requires implementing only the transport contract and its template, with zero modifications to the core notification service.
- **SC-005**: All automated verification suites (Checkstyle, PMD, SpotBugs, ArchUnit, unit and integration tests) pass with 100% compliance and zero warnings.
