package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import es.nachobrito.sdd.ecommerce.domain.model.Product;
import es.nachobrito.sdd.ecommerce.domain.model.ProductStatus;
import es.nachobrito.sdd.ecommerce.domain.port.outbound.ProductRepositoryPort;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing ProductRepositoryPort using Micronaut Data JDBC.
 */
@Singleton
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final MicronautProductRepository repository;

    /**
     * Constructs the persistence adapter with explicit constructor injection.
     *
     * @param repository Compile-time Micronaut Data JDBC repository
     */
    public ProductPersistenceAdapter(final MicronautProductRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Product save(final Product product) {
        Objects.requireNonNull(product, "product must not be null");
        final ProductEntity entity = toEntity(product);
        final ProductEntity saved = repository.existsById(product.id())
                ? repository.update(entity)
                : repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Product> findById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Product> findBySku(final String sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        return repository.findBySku(sku).map(this::toDomain);
    }

    @Override
    public List<Product> findAll() {
        final List<Product> products = new ArrayList<>();
        for (final ProductEntity entity : repository.findAll()) {
            products.add(toDomain(entity));
        }
        return List.copyOf(products);
    }

    private ProductEntity toEntity(final Product domain) {
        return new ProductEntity(
                domain.id(),
                domain.sku(),
                domain.name(),
                domain.description(),
                domain.price(),
                domain.currency(),
                domain.status().name(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }

    private Product toDomain(final ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getSku(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCurrency(),
                ProductStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
