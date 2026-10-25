# Quickstart: Orders Management

**Feature**: Orders Management  
**Branch**: `003-orders-management`  

This guide provides testing steps and curl examples for interacting with the Orders Management API.

## Prerequisites

Start the application locally:
```bash
make dev # or ./gradlew run
```

The application runs at `http://localhost:8080`.

---

## 1. Create an Order (POST `/v1/orders`)

```bash
curl -i -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "id": "c1f78086-4ef5-46aa-b2b9-e48325a62174",
    "deliveryAddress": {
      "name": "Alice Smith",
      "telephone": "+34600112233",
      "street": "Gran Via 42",
      "complement": "Flat 3B",
      "city": "Madrid",
      "state": "Madrid",
      "zipCode": "28013",
      "country": "Spain"
    },
    "invoiceAddress": {
      "name": "Alice Smith Consulting",
      "vatNumber": "ESA12345678",
      "telephone": "+34911223344",
      "street": "Gran Via 42",
      "complement": "Flat 3B",
      "city": "Madrid",
      "state": "Madrid",
      "zipCode": "28013",
      "country": "Spain"
    },
    "lines": [
      {
        "productId": "7e59676e-82b2-4d2c-880c-7871b6d05f32",
        "quantity": 2,
        "unitPriceInCents": 1999,
        "lineTotalInCents": 3998
      }
    ],
    "totalAmountInCents": 3998,
    "currency": "EUR"
  }'
```

**Expected Response**: `201 Created`
```json
{
  "id": "c1f78086-4ef5-46aa-b2b9-e48325a62174",
  "date": "2026-10-25T14:30:00Z",
  "status": "CREATED",
  "totalAmountInCents": 3998,
  "currency": "EUR",
  "deliveryAddress": {
    "name": "Alice Smith",
    "telephone": "+34600112233",
    "street": "Gran Via 42",
    "complement": "Flat 3B",
    "city": "Madrid",
    "state": "Madrid",
    "zipCode": "28013",
    "country": "Spain"
  },
  "invoiceAddress": {
    "name": "Alice Smith Consulting",
    "vatNumber": "ESA12345678",
    "telephone": "+34911223344",
    "street": "Gran Via 42",
    "complement": "Flat 3B",
    "city": "Madrid",
    "state": "Madrid",
    "zipCode": "28013",
    "country": "Spain"
  },
  "lines": [
    {
      "productId": "7e59676e-82b2-4d2c-880c-7871b6d05f32",
      "quantity": 2,
      "unitPriceInCents": 1999,
      "lineTotalInCents": 3998
    }
  ]
}
```

---

## 2. Retrieve an Order by ID (GET `/v1/orders/{id}`)

```bash
curl -i -X GET http://localhost:8080/v1/orders/c1f78086-4ef5-46aa-b2b9-e48325a62174
```

**Expected Response**: `200 OK`

---

## 3. List All Orders (GET `/v1/orders`)

```bash
curl -i -X GET http://localhost:8080/v1/orders
```

**Expected Response**: `200 OK` with an array of orders.

---

## 4. Run Automated Verification

```bash
# Run unit, integration, and architecture tests
make check # or ./gradlew check
```
