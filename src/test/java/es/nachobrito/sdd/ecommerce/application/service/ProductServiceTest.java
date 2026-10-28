package es.nachobrito.sdd.ecommerce.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.nachobrito.sdd.ecommerce.domain.event.ProductCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.exception.ProductAlreadyExistsException;
import es.nachobrito.sdd.ecommerce.domain.model.Product;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.EventPublisherPort;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.ProductRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DisplayName("ProductService Unit Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class ProductServiceTest {

    private ProductRepositoryPort productRepository;
    private EventPublisherPort eventPublisher;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepositoryPort.class);
        eventPublisher = Mockito.mock(EventPublisherPort.class);
        productService = new ProductService(productRepository, eventPublisher);
    }

    @Test
    @DisplayName("Should create product successfully and publish ProductCreatedEvent")
    void shouldCreateProductSuccessfully() {
        final UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final Product created = productService.createProduct(
                id, "SKU-999", "Mouse Pad", "Ergonomic pad", Product.MIN_PRICE_IN_CENTS, "EUR"
        );

        assertNotNull(created);
        assertEquals(id, created.id());
        assertEquals("SKU-999", created.sku());
        assertEquals(Product.MIN_PRICE_IN_CENTS, created.priceInCents());

        verify(productRepository).save(any(Product.class));

        final ArgumentCaptor<ProductCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        final ProductCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(id, publishedEvent.productId());
        assertEquals("SKU-999", publishedEvent.sku());
    }

    @Test
    @DisplayName("Should reject product creation with null or non-positive price")
    void shouldRejectInvalidPriceInCents() {
        final UUID id = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
                id, "SKU-1", "Name", "Desc", null, "EUR"
        ));

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
                id, "SKU-1", "Name", "Desc", 0, "EUR"
        ));

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
                id, "SKU-1", "Name", "Desc", -10, "EUR"
        ));

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(ProductCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw ProductAlreadyExistsException when product with ID already exists")
    void shouldThrowExceptionWhenProductAlreadyExists() {
        final UUID id = UUID.randomUUID();
        final Product existing = Product.create(id, "SKU-EXISTING", "Name", "Desc", 1000, "EUR");
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        final ProductAlreadyExistsException ex = assertThrows(
                ProductAlreadyExistsException.class,
                () -> productService.createProduct(id, "SKU-NEW", "Name", "Desc", 2000, "EUR")
        );

        assertEquals(id, ex.getProductId());
        assertTrue(ex.getMessage().contains(id.toString()));
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(ProductCreatedEvent.class));
    }

    @Test
    @DisplayName("Should retrieve product by ID when present")
    void shouldGetProductById() {
        final UUID id = UUID.randomUUID();
        final Product product = Product.create(id, "SKU-123", "Laptop", "Laptop stand", 5000, "EUR");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        final Optional<Product> result = productService.getById(id);

        assertTrue(result.isPresent());
        assertEquals("Laptop", result.get().name());
        verify(productRepository).findById(id);
    }

    @Test
    @DisplayName("Should list all products")
    void shouldListAllProducts() {
        final Product p1 = Product.create(UUID.randomUUID(), "SKU-1", "Product 1", "Desc", 1000, "EUR");
        final Product p2 = Product.create(UUID.randomUUID(), "SKU-2", "Product 2", "Desc", 2000, "EUR");
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        final List<Product> products = productService.listAll();

        assertEquals(2, products.size());
        assertEquals("SKU-1", products.get(0).sku());
        assertEquals("SKU-2", products.get(1).sku());
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should reject null dependencies in constructor")
    void shouldRejectNullDependenciesInConstructor() {
        assertThrows(NullPointerException.class, () -> new ProductService(null, eventPublisher));
        assertThrows(NullPointerException.class, () -> new ProductService(productRepository, null));
    }
}
