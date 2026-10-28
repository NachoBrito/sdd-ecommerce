package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.nachobrito.sdd.ecommerce.domain.model.Product;
import es.nachobrito.sdd.ecommerce.domain.model.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DisplayName("ProductPersistenceAdapter Unit Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class ProductPersistenceAdapterTest {

    private MicronautProductRepository repository;
    private ProductPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(MicronautProductRepository.class);
        adapter = new ProductPersistenceAdapter(repository);
    }

    @Test
    @DisplayName("Should save new product via repository.save() when entity does not exist")
    void shouldSaveNewProductWhenNotExists() {
        final UUID id = UUID.randomUUID();
        final Instant now = Instant.now();
        final Product product = new Product(
                id, "SKU-SAVE-1", "Product Name", "Detailed description",
                Product.MIN_PRICE_IN_CENTS, "EUR", ProductStatus.DRAFT, now, now
        );
        final ProductEntity entity = new ProductEntity(
                id, "SKU-SAVE-1", "Product Name", "Detailed description",
                Product.MIN_PRICE_IN_CENTS, "EUR", "DRAFT", now, now
        );

        when(repository.existsById(id)).thenReturn(false);
        when(repository.save(any(ProductEntity.class))).thenReturn(entity);

        final Product saved = adapter.save(product);

        assertNotNull(saved);
        assertEquals(id, saved.id());
        assertEquals("Detailed description", saved.description());
        assertEquals(Product.MIN_PRICE_IN_CENTS, saved.priceInCents());
        verify(repository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update existing product via repository.update() when entity exists")
    void shouldUpdateExistingProductWhenExists() {
        final UUID id = UUID.randomUUID();
        final Instant now = Instant.now();
        final Product product = new Product(
                id, "SKU-UPDATE-1", "Updated Name", "Updated description",
                5000, "EUR", ProductStatus.ACTIVE, now, now
        );
        final ProductEntity entity = new ProductEntity(
                id, "SKU-UPDATE-1", "Updated Name", "Updated description",
                5000, "EUR", "ACTIVE", now, now
        );

        when(repository.existsById(id)).thenReturn(true);
        when(repository.update(any(ProductEntity.class))).thenReturn(entity);

        final Product updated = adapter.save(product);

        assertNotNull(updated);
        assertEquals(id, updated.id());
        assertEquals("Updated Name", updated.name());
        assertEquals("Updated description", updated.description());
        assertEquals(ProductStatus.ACTIVE, updated.status());
        verify(repository).update(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should reject product with price below minimum in save")
    void shouldRejectInvalidPriceInSave() {
        final UUID id = UUID.randomUUID();
        final Product invalidProduct = Mockito.mock(Product.class);
        when(invalidProduct.id()).thenReturn(id);
        when(invalidProduct.priceInCents()).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> adapter.save(invalidProduct));
    }

    @Test
    @DisplayName("Should find product by ID and map to domain")
    void shouldFindById() {
        final UUID id = UUID.randomUUID();
        final Instant now = Instant.now();
        final ProductEntity entity = new ProductEntity(
                id, "SKU-FIND-1", "Find Name", "Find description",
                2500, "EUR", "DRAFT", now, now
        );
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        final Optional<Product> found = adapter.findById(id);

        assertTrue(found.isPresent());
        assertEquals("SKU-FIND-1", found.get().sku());
        assertEquals("Find description", found.get().description());
    }

    @Test
    @DisplayName("Should find product by SKU and map to domain")
    void shouldFindBySku() {
        final UUID id = UUID.randomUUID();
        final Instant now = Instant.now();
        final ProductEntity entity = new ProductEntity(
                id, "SKU-BY-SKU", "SKU Product", "SKU description",
                3000, "EUR", "ACTIVE", now, now
        );
        when(repository.findBySku("SKU-BY-SKU")).thenReturn(Optional.of(entity));

        final Optional<Product> found = adapter.findBySku("SKU-BY-SKU");

        assertTrue(found.isPresent());
        assertEquals(id, found.get().id());
        assertEquals("SKU Product", found.get().name());

        when(repository.findBySku("UNKNOWN")).thenReturn(Optional.empty());
        assertFalse(adapter.findBySku("UNKNOWN").isPresent());
    }

    @Test
    @DisplayName("Should find all products and map to domain list")
    void shouldFindAll() {
        final Instant now = Instant.now();
        final ProductEntity e1 = new ProductEntity(
                UUID.randomUUID(), "SKU-ALL-1", "P1", "D1", 1000, "EUR", "DRAFT", now, now
        );
        final ProductEntity e2 = new ProductEntity(
                UUID.randomUUID(), "SKU-ALL-2", "P2", "D2", 2000, "EUR", "ACTIVE", now, now
        );
        when(repository.findAll()).thenReturn(List.of(e1, e2));

        final List<Product> all = adapter.findAll();

        assertEquals(2, all.size());
        assertEquals("SKU-ALL-1", all.get(0).sku());
        assertEquals("SKU-ALL-2", all.get(1).sku());
    }

    @Test
    @DisplayName("Should reject null repository in constructor")
    void shouldRejectNullRepository() {
        assertThrows(NullPointerException.class, () -> new ProductPersistenceAdapter(null));
    }
}
