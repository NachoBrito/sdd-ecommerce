package es.nachobrito.sdd.ecommerce.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.nachobrito.sdd.ecommerce.domain.model.Product;
import es.nachobrito.sdd.ecommerce.domain.model.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Pure unit tests for Product aggregate root business transformations.
 */
class ProductTest {

    private static final String DEFAULT_CURRENCY = "USD";

    @Test
    @DisplayName("Should create product with valid parameters in DRAFT status")
    void shouldCreateProductInDraftStatus() {
        final Product product = Product.create(
                "SKU-1001",
                "Ergonomic Keyboard",
                "High quality wireless keyboard",
                new BigDecimal("129.99"),
                DEFAULT_CURRENCY
        );

        assertAll("Product creation assertions",
                () -> assertNotNull(product.id(), "Product ID must not be null"),
                () -> assertEquals("SKU-1001", product.sku(), "SKU must match expected value"),
                () -> assertEquals("Ergonomic Keyboard", product.name(), "Name must match expected value"),
                () -> assertEquals(ProductStatus.DRAFT, product.status(), "Status must be DRAFT upon creation"),
                () -> assertNotNull(product.createdAt(), "createdAt timestamp must not be null"),
                () -> assertNotNull(product.updatedAt(), "updatedAt timestamp must not be null")
        );
    }

    @Test
    @DisplayName("Should activate draft product")
    void shouldActivateProduct() {
        final Product product = Product.create(
                "SKU-1002",
                "Wireless Mouse",
                "Precision optical mouse",
                new BigDecimal("49.99"),
                DEFAULT_CURRENCY
        );

        final Product activated = product.activate();
        assertEquals(ProductStatus.ACTIVE, activated.status(), "Status must transition to ACTIVE");
    }

    @Test
    @DisplayName("Should archive product")
    void shouldArchiveProduct() {
        final Product product = Product.create(
                "SKU-1003",
                "USB-C Cable",
                "Fast charging cable",
                new BigDecimal("19.99"),
                DEFAULT_CURRENCY
        );

        final Product archived = product.archive();
        assertEquals(ProductStatus.ARCHIVED, archived.status(), "Status must transition to ARCHIVED");
    }

    @Test
    @DisplayName("Should reject invalid price below minimum")
    void shouldRejectInvalidPrice() {
        assertThrows(IllegalArgumentException.class, () -> Product.create(
                "SKU-1004",
                "Monitor",
                "4K Monitor",
                BigDecimal.ZERO,
                DEFAULT_CURRENCY
        ), "Creating product with zero price must fail");
    }

    @Test
    @DisplayName("Should reject invalid or empty SKU")
    void shouldRejectEmptySku() {
        assertThrows(IllegalArgumentException.class, () -> Product.create(
                "",
                "Desk Mat",
                "Felt desk mat",
                new BigDecimal("29.99"),
                DEFAULT_CURRENCY
        ), "Creating product with empty SKU must fail");
    }
}
