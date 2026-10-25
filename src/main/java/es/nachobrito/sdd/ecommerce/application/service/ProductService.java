package es.nachobrito.sdd.ecommerce.application.service;

import es.nachobrito.sdd.ecommerce.application.aop.StructuredLog;
import es.nachobrito.sdd.ecommerce.domain.event.ProductCreatedEvent;
import es.nachobrito.sdd.ecommerce.domain.exception.ProductAlreadyExistsException;
import es.nachobrito.sdd.ecommerce.domain.model.Product;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.CreateProductUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.GetProductUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.EventPublisherPort;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.ProductRepositoryPort;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating Product use cases.
 */
@Singleton
@StructuredLog
public class ProductService implements CreateProductUseCase, GetProductUseCase {

    private final ProductRepositoryPort productRepository;
    private final EventPublisherPort eventPublisher;

    /**
     * Constructs the application service with explicit constructor injection.
     *
     * @param productRepository Outbound persistence repository port
     * @param eventPublisher Outbound event publisher port
     */
    public ProductService(
            final ProductRepositoryPort productRepository,
            final EventPublisherPort eventPublisher
    ) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public Product createProduct(
            final UUID id,
            final String sku,
            final String name,
            final String description,
            final Integer priceInCents,
            final String currency
    ) {
        Objects.requireNonNull(id, "Product id must not be null");
        if (priceInCents == null || priceInCents < Product.MIN_PRICE_IN_CENTS) {
            throw new IllegalArgumentException("Product price in cents must be at least " + Product.MIN_PRICE_IN_CENTS);
        }

        if (productRepository.findById(id).isPresent()) {
            throw new ProductAlreadyExistsException(id);
        }

        final Product product = Product.create(id, sku, name, description, priceInCents, currency);
        final Product saved = productRepository.save(product);
        final ProductCreatedEvent event = ProductCreatedEvent.create(saved.id(), saved.sku(), null);
        eventPublisher.publish(event);
        return saved;
    }

    @Override
    public Optional<Product> getById(final UUID id) {
        return productRepository.findById(id);
    }

    @Override
    public List<Product> listAll() {
        return productRepository.findAll();
    }
}
