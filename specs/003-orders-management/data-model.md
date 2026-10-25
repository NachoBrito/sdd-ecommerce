# Data Model: Orders Management

**Feature**: Orders Management  
**Branch**: `003-orders-management`  
**Date**: 2026-10-25  

## Domain Model (Pure Java Records - Zero Frameworks)

### `Order` (Aggregate Root)
```java
public record Order(
    UUID id,
    String customerName,
    String customerEmail,
    Instant date,
    OrderStatus status,
    Integer totalAmountInCents,
    String currency,
    DeliveryAddress deliveryAddress,
    InvoiceAddress invoiceAddress,
    List<OrderLine> lines
) {
    // Invariants enforced in compact constructor:
    // - id != null
    // - customerName != null && !customerName.isBlank()
    // - customerEmail != null && valid email pattern
    // - date != null
    // - status != null
    // - currency != null && currency.matches("^[A-Z]{3}$")
    // - deliveryAddress != null
    // - invoiceAddress != null
    // - lines != null && !lines.isEmpty()
    // - totalAmountInCents != null && totalAmountInCents > 0
    // - totalAmountInCents == sum of lines' lineTotalInCents
}
```

### `OrderStatus` (Enum)
```java
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
```

### `DeliveryAddress` (Value Object)
```java
public record DeliveryAddress(
    String name,
    String telephone,
    String street,
    String complement,
    String city,
    String state,
    String zipCode,
    String country
) {
    // Invariants:
    // - name != null && !name.isBlank() (max 128)
    // - telephone != null && !telephone.isBlank() (max 32)
    // - street != null && !street.isBlank() (max 255)
    // - complement is optional (max 255)
    // - city != null && !city.isBlank() (max 100)
    // - state != null && !state.isBlank() (max 100)
    // - zipCode != null && !zipCode.isBlank() (max 20)
    // - country != null && !country.isBlank() (max 100)
}
```

### `InvoiceAddress` (Value Object)
```java
public record InvoiceAddress(
    String name,
    String vatNumber,
    String telephone,
    String street,
    String complement,
    String city,
    String state,
    String zipCode,
    String country
) {
    // Invariants:
    // - name != null && !name.isBlank() (max 128)
    // - vatNumber != null && !vatNumber.isBlank() (max 32)
    // - telephone != null && !telephone.isBlank() (max 32)
    // - street != null && !street.isBlank() (max 255)
    // - complement is optional (max 255)
    // - city != null && !city.isBlank() (max 100)
    // - state != null && !state.isBlank() (max 100)
    // - zipCode != null && !zipCode.isBlank() (max 20)
    // - country != null && !country.isBlank() (max 100)
}
```

### `OrderLine` (Entity within Aggregate)
```java
public record OrderLine(
    UUID productId,
    Integer quantity,
    Integer unitPriceInCents,
    Integer lineTotalInCents
) {
    // Invariants:
    // - productId != null
    // - quantity != null && quantity >= 1
    // - unitPriceInCents != null && unitPriceInCents >= 1
    // - lineTotalInCents != null && lineTotalInCents == quantity * unitPriceInCents
}
```

---

## REST DTOs (Boundary Layer)

### `CreateOrderRequest`
```java
@Serdeable
public record CreateOrderRequest(
    @NotNull UUID id,
    @NotBlank String customerName,
    @NotBlank @Email String customerEmail,
    @NotNull @Valid DeliveryAddressDto deliveryAddress,
    @NotNull @Valid InvoiceAddressDto invoiceAddress,
    @NotEmpty @Valid List<OrderLineDto> lines,
    @NotNull @Min(1) Integer totalAmountInCents,
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency
) {}
```

### `DeliveryAddressDto`
```java
@Serdeable
public record DeliveryAddressDto(
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 32) String telephone,
    @NotBlank @Size(max = 255) String street,
    @Size(max = 255) String complement,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Size(max = 20) String zipCode,
    @NotBlank @Size(max = 100) String country
) {}
```

### `InvoiceAddressDto`
```java
@Serdeable
public record InvoiceAddressDto(
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 32) String vatNumber,
    @NotBlank @Size(max = 32) String telephone,
    @NotBlank @Size(max = 255) String street,
    @Size(max = 255) String complement,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Size(max = 20) String zipCode,
    @NotBlank @Size(max = 100) String country
) {}
```

### `OrderLineDto`
```java
@Serdeable
public record OrderLineDto(
    @NotNull UUID productId,
    @NotNull @Min(1) Integer quantity,
    @NotNull @Min(1) Integer unitPriceInCents,
    @NotNull @Min(1) Integer lineTotalInCents
) {}
```

### `OrderResponse`
```java
@Serdeable
public record OrderResponse(
    UUID id,
    String customerName,
    String customerEmail,
    Instant date,
    String status,
    Integer totalAmountInCents,
    String currency,
    DeliveryAddressDto deliveryAddress,
    InvoiceAddressDto invoiceAddress,
    List<OrderLineDto> lines
) {}
```

---

## Database Relational Schema (MariaDB)

### `orders` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PRIMARY KEY | Client-provided UUID |
| `customer_name` | VARCHAR(128) | NOT NULL | Customer full name |
| `customer_email` | VARCHAR(255) | NOT NULL | Customer email address |
| `date` | TIMESTAMP(6) | NOT NULL | Order creation instant (UTC) |
| `status` | VARCHAR(32) | NOT NULL | Enum representation (`CREATED`, etc.) |
| `total_amount_in_cents` | INT | NOT NULL | Sum of line totals in cents |
| `currency` | VARCHAR(3) | NOT NULL | ISO-4217 currency code |
| `delivery_name` | VARCHAR(128) | NOT NULL | Recipient name |
| `delivery_telephone` | VARCHAR(32) | NOT NULL | Delivery phone |
| `delivery_street` | VARCHAR(255) | NOT NULL | Delivery address line |
| `delivery_complement` | VARCHAR(255) | NULL | Delivery complement/suite |
| `delivery_city` | VARCHAR(100) | NOT NULL | Delivery city |
| `delivery_state` | VARCHAR(100) | NOT NULL | Delivery state/province |
| `delivery_zip_code` | VARCHAR(20) | NOT NULL | Delivery postal code |
| `delivery_country` | VARCHAR(100) | NOT NULL | Delivery country |
| `invoice_name` | VARCHAR(128) | NOT NULL | Invoicing entity name |
| `invoice_vat_number` | VARCHAR(32) | NOT NULL | Tax/VAT identification number |
| `invoice_telephone` | VARCHAR(32) | NOT NULL | Billing telephone |
| `invoice_street` | VARCHAR(255) | NOT NULL | Billing address line |
| `invoice_complement` | VARCHAR(255) | NULL | Billing complement/suite |
| `invoice_city` | VARCHAR(100) | NOT NULL | Billing city |
| `invoice_state` | VARCHAR(100) | NOT NULL | Billing state/province |
| `invoice_zip_code` | VARCHAR(20) | NOT NULL | Billing postal code |
| `invoice_country` | VARCHAR(100) | NOT NULL | Billing country |
| `created_at` | TIMESTAMP(6) | NOT NULL DEFAULT CURRENT_TIMESTAMP(6) | Audit creation timestamp |
| `updated_at` | TIMESTAMP(6) | NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | Audit update timestamp |

### `order_lines` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PRIMARY KEY | Line item primary key (UUID) |
| `order_id` | VARCHAR(36) | NOT NULL, INDEX | References `orders(id)` ON DELETE CASCADE |
| `product_id` | VARCHAR(36) | NOT NULL | Referenced product UUID |
| `quantity` | INT | NOT NULL | Quantity ordered (>= 1) |
| `unit_price_in_cents` | INT | NOT NULL | Unit price in integer cents |
| `line_total_in_cents` | INT | NOT NULL | Line total (`quantity * unit_price_in_cents`) |
