# Quickstart: Order Notifications

**Feature**: Order Notifications  
**Branch**: `004-order-notifications`  

This guide provides testing steps, verification procedures, and local configuration for the Order Notifications feature.

---

## Prerequisites

1. Start a local SMTP testing server (e.g., MailHog, Mailpit, or in-memory test mail server):
```bash
# Using Mailpit via Docker (optional for visual inspection):
docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit
```

2. Start the e-commerce service:
```bash
./gradlew run
```

Application runs at `http://localhost:8080`.

---

## 1. Triggering an Order Notification

Place an order via the REST API (`POST /v1/orders`):

```bash
curl -i -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "id": "a2c89197-5ef6-47bb-c3c0-f59436b73285",
    "customerName": "Jane Doe",
    "customerEmail": "jane.doe@example.com",
    "deliveryAddress": {
      "name": "Jane Doe",
      "telephone": "+34611223344",
      "street": "Paseo de la Castellana 100",
      "complement": "Floor 4, Apt A",
      "city": "Madrid",
      "state": "Madrid",
      "zipCode": "28046",
      "country": "Spain"
    },
    "invoiceAddress": {
      "name": "Jane Doe",
      "vatNumber": "ESB98765432",
      "telephone": "+34611223344",
      "street": "Paseo de la Castellana 100",
      "complement": "Floor 4, Apt A",
      "city": "Madrid",
      "state": "Madrid",
      "zipCode": "28046",
      "country": "Spain"
    },
    "lines": [
      {
        "productId": "7e59676e-82b2-4d2c-880c-7871b6d05f32",
        "quantity": 1,
        "unitPriceInCents": 2999,
        "lineTotalInCents": 2999
      }
    ],
    "totalAmountInCents": 2999,
    "currency": "EUR"
  }'
```

---

## 2. Expected Behavior

1. **Order Creation Response**: HTTP `201 Created` with order payload.
2. **Event Emission**: `OrderCreatedEvent` is published to the application event bus.
3. **Notification Processing**:
   - `OrderCreatedNotificationListener` handles the event.
   - `NotificationService` retrieves order `a2c89197-5ef6-47bb-c3c0-f59436b73285`.
   - `SmtpNotificationTransport` compiles the HTML email using `templates/order-confirmation.html`.
   - Sends the message to `jane.doe@example.com` with subject: `Order Confirmation - a2c89197-5ef6-47bb-c3c0-f59436b73285`.
4. **Log Inspection**:
   ```text
   INFO  e.n.s.e.a.s.NotificationService - Dispatched order notification to 1 transport(s) for orderId=a2c89197-5ef6-47bb-c3c0-f59436b73285
   INFO  e.n.s.e.i.a.o.n.s.SmtpNotificationTransport - Email delivered successfully to jane.doe@example.com for orderId=a2c89197-5ef6-47bb-c3c0-f59436b73285
   ```

---

## 3. Running Automated Checks

```bash
# Run unit & integration tests including email delivery tests:
./gradlew check
```
