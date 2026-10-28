package es.nachobrito.sdd.ecommerce.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.nachobrito.sdd.ecommerce.domain.model.Product;
import es.nachobrito.sdd.ecommerce.domain.model.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Pure unit tests for Product aggregate root business transformations.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ProductTest {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final int VALID_PRICE_IN_CENTS = 12_999;

    @Test
    @DisplayName("Should create product with valid parameters in DRAFT status")
    void shouldCreateProductInDraftStatus() {
        final UUID id = UUID.randomUUID();
        final Product product = Product.create(
                id,
                "SKU-1001",
                "Ergonomic Keyboard",
                "High quality wireless keyboard",
                VALID_PRICE_IN_CENTS,
                DEFAULT_CURRENCY
        );

        assertAll("Product creation assertions",
                () -> assertEquals(id, product.id(), "Product ID must match client-supplied UUID"),
                () -> assertEquals("SKU-1001", product.sku(), "SKU must match expected value"),
                () -> assertEquals("Ergonomic Keyboard", product.name(), "Name must match expected value"),
                () -> assertEquals(VALID_PRICE_IN_CENTS, product.priceInCents(), "priceInCents must match expected value"),
                () -> assertEquals(ProductStatus.DRAFT, product.status(), "Status must be DRAFT upon creation"),
                () -> assertNotNull(product.createdAt(), "createdAt timestamp must not be null"),
                () -> assertNotNull(product.updatedAt(), "updatedAt timestamp must not be null")
        );
    }

    @Test
    @DisplayName("Should activate draft product")
    void shouldActivateProduct() {
        final UUID id = UUID.randomUUID();
        final Product product = Product.create(
                id,
                "SKU-1002",
                "Wireless Mouse",
                "Precision optical mouse",
                4_999,
                DEFAULT_CURRENCY
        );

        final Product activated = product.activate();
        assertEquals(ProductStatus.ACTIVE, activated.status(), "Status must transition to ACTIVE");
        assertEquals(id, activated.id(), "Product ID must be preserved after activation");
        assertEquals(4_999, activated.priceInCents(), "priceInCents must be preserved after activation");
    }

    @Test
    @DisplayName("Should archive product")
    void shouldArchiveProduct() {
        final UUID id = UUID.randomUUID();
        final Product product = Product.create(
                id,
                "SKU-1003",
                "USB-C Cable",
                "Fast charging cable",
                1_999,
                DEFAULT_CURRENCY
        );

        final Product archived = product.archive();
        assertEquals(ProductStatus.ARCHIVED, archived.status(), "Status must transition to ARCHIVED");
        assertEquals(id, archived.id(), "Product ID must be preserved after archiving");
        assertEquals(1_999, archived.priceInCents(), "priceInCents must be preserved after archiving");
    }

    @Test
    @DisplayName("Should reject null product ID")
    void shouldRejectNullProductId() {
        assertThrows(NullPointerException.class, () -> Product.create(
                null,
                "SKU-1004",
                "Monitor",
                "4K Monitor",
                29_999,
                DEFAULT_CURRENCY
        ), "Creating product with null ID must fail");
    }

    @Test
    @DisplayName("Should reject invalid price in cents below minimum")
    void shouldRejectInvalidPriceInCents() {
        final UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> Product.create(
                id,
                "SKU-1004",
                "Monitor",
                "4K Monitor",
                0,
                DEFAULT_CURRENCY
        ), "Creating product with zero price in cents must fail");

        assertThrows(IllegalArgumentException.class, () -> Product.create(
                id,
                "SKU-1004",
                "Monitor",
                "4K Monitor",
                -100,
                DEFAULT_CURRENCY
        ), "Creating product with negative price in cents must fail");
    }

    @Test
    @DisplayName("Should accept product with exact minimum price in cents")
    void shouldAcceptExactMinimumPriceInCents() {
        final UUID id = UUID.randomUUID();
        final Product product = Product.create(
                id,
                "SKU-MIN-1",
                "Penny Candy",
                "Sample product at minimum price",
                Product.MIN_PRICE_IN_CENTS,
                DEFAULT_CURRENCY
        );
        assertEquals(Product.MIN_PRICE_IN_CENTS, product.priceInCents(), "Product price must match exact minimum");
    }

    @Test
    @DisplayName("Should reject invalid or empty SKU")
    void shouldRejectEmptySku() {
        final UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> Product.create(
                id,
                "",
                "Desk Mat",
                "Felt desk mat",
                2_999,
                DEFAULT_CURRENCY
        ), "Creating product with empty SKU must fail");
    }
}
