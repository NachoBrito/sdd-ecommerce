# Data Model & Domain Entities: Order Notifications

**Feature Branch**: `004-order-notifications`  
**Date**: 2026-10-25  
**Spec**: [spec.md](./spec.md)

---

## 1. Domain Entities & Value Objects

The notification domain is defined in `es.nachobrito.sdd.ecommerce.domain.model` using pure, immutable Java records with zero framework dependencies.

### 1.1 `OrderNotification` (Aggregate / Value Object)

Encapsulates all data required to render and dispatch an order notification across any transport medium.

```java
public record OrderNotification(
    UUID orderId,
    String customerName,
    String customerEmail,
    Instant orderDate,
    String status,
    Integer totalAmountInCents,
    String currency,
    NotificationAddress deliveryAddress,
    NotificationAddress invoiceAddress,
    List<NotificationLineItem> lines,
    String traceId
) {
    // Invariants enforced in compact constructor:
    // - orderId, customerName, customerEmail, orderDate, status, totalAmountInCents, currency, lines != null
    // - customerEmail is valid email syntax
    // - lines is unmodifiable and non-empty
    // - totalAmountInCents >= 1
}
```

### 1.2 `NotificationAddress` (Value Object)

Represents destination address details formatted for notification presentation.

```java
public record NotificationAddress(
    String name,
    String telephone,
    String street,
    String complement,
    String city,
    String state,
    String zipCode,
    String country,
    String vatNumber // Present for invoice address, null for delivery address
) {
    // Helper method to produce a clean multi-line string with null/empty complement handled
    public String formattedLines();
}
```

### 1.3 `NotificationLineItem` (Value Object)

Represents an individual purchased product item within the notification.

```java
public record NotificationLineItem(
    UUID productId,
    Integer quantity,
    Integer unitPriceInCents,
    Integer lineTotalInCents
) {
    // Formatted monetary helpers, e.g., formattedUnitAmount(currency), formattedLineTotal(currency)
}
```

---

## 2. Inbound & Outbound Ports

Following Hexagonal Architecture:

### 2.1 Inbound Port: `SendOrderNotificationUseCase`
Located in `es.nachobrito.sdd.ecommerce.domain.port.inbound`:

```java
@FunctionalInterface
public interface SendOrderNotificationUseCase {
    /**
     * Dispatches notifications for a newly created order across all registered active transports.
     *
     * @param orderId Identifier of the created order
     * @param traceId Correlation trace ID
     */
    void sendOrderNotification(UUID orderId, String traceId);
}
```

### 2.2 Outbound Port: `NotificationTransportPort`
Located in `es.nachobrito.sdd.ecommerce.domain.port.outbound`:

```java
public interface NotificationTransportPort {
    /**
     * Unique transport identifier (e.g., "SMTP", "SMS").
     */
    String transportName();

    /**
     * Indicates whether this transport is currently active and configured.
     */
    boolean isEnabled();

    /**
     * Dispatches an order notification using this transport's medium and dedicated template.
     *
     * @param notification Notification data model
     */
    void send(OrderNotification notification);
}
```

---

## 3. Infrastructure Adapter & Template Model

### 3.1 `SmtpNotificationTransport`
Located in `es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp`:
- Implements `NotificationTransportPort`
- Injects `SmtpHtmlTemplateRenderer` and `SmtpSender` / mail session
- Renders HTML content via `SmtpHtmlTemplateRenderer`
- Sends MIME message with `text/html; charset=UTF-8`

### 3.2 HTML Email Template Variables
The HTML template located at `src/main/resources/templates/order-confirmation.html` defines the presentation structure:

| Token | Source Field | Description | Escaped? |
|-------|--------------|-------------|----------|
| `{{orderId}}` | `notification.orderId()` | Order UUID | Yes |
| `{{orderDate}}` | `notification.orderDate()` | ISO / formatted UTC date | Yes |
| `{{customerName}}` | `notification.customerName()` | Full name of recipient | Yes |
| `{{customerEmail}}` | `notification.customerEmail()` | Email address of recipient | Yes |
| `{{currency}}` | `notification.currency()` | ISO-4217 currency code | Yes |
| `{{totalAmount}}` | `notification.totalAmountInCents()` | Formatted monetary amount (e.g. "€25.00") | Yes |
| `{{deliveryAddress}}` | `notification.deliveryAddress()` | Formatted HTML address block | Yes |
| `{{invoiceAddress}}` | `notification.invoiceAddress()` | Formatted HTML address block with VAT | Yes |
| `{{orderLines}}` | `notification.lines()` | HTML `<tr>` rows with item details | Pre-rendered rows |
