# Quickstart: Product Aggregate Updates (Client-Side ID & Price in Cents)

**Feature Branch**: `002-update-product-aggregate`  
**Date**: 2026-10-25  

## Summary of Changes

This feature updates the Product aggregate to:
1. Accept and require a client-generated UUID `id` parameter upon creation.
2. Store and express prices strictly as whole integer cents (`priceInCents`).
3. Enforce strict multi-layer validation across controller, application/service, domain model, and repository layers.

## Verification Commands

Execute the standard verification commands to confirm all tests, static analysis, and architectural boundaries pass:

```bash
# Run all quality checks (Checkstyle, PMD, SpotBugs, ArchUnit)
make check
# OR
./gradlew check

# Run tests including integration and unit tests
make test
# OR
./gradlew test
```

## Example API Usage

### Create Product with Client-Generated ID & Price in Cents

```bash
curl -X POST http://localhost:8080/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "sku": "KB-MECH-01",
    "name": "Custom Mechanical Keyboard",
    "description": "Hot-swappable mechanical keyboard",
    "priceInCents": 14999,
    "currency": "USD"
  }'
```

**Expected Response (201 Created)**:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "sku": "KB-MECH-01",
  "name": "Custom Mechanical Keyboard",
  "description": "Hot-swappable mechanical keyboard",
  "priceInCents": 14999,
  "currency": "USD",
  "status": "DRAFT",
  "createdAt": "2026-10-25T12:00:00Z",
  "updatedAt": "2026-10-25T12:00:00Z"
}
```

### Invalid Requests Rejection (400 Bad Request)

- Missing `id`:
  ```json
  {"message": "id: ID is required", "status": 400}
  ```
- Non-positive `priceInCents`:
  ```json
  {"message": "priceInCents: priceInCents must be at least 1 cent", "status": 400}
  ```

### Duplicate ID Rejection (409 Conflict)

Submitting an existing ID returns:
```json
{
  "message": "Product with ID '123e4567-e89b-12d3-a456-426614174000' already exists",
  "status": 409
}
```
