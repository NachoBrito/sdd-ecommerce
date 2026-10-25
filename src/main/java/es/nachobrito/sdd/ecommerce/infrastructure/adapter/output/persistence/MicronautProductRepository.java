package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Compile-time Micronaut Data JDBC repository for products table.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface MicronautProductRepository extends CrudRepository<ProductEntity, UUID> {

    /**
     * Finds a product entity by its unique SKU.
     *
     * @param sku The stock keeping unit
     * @return Optional containing the product entity if found
     */
    Optional<ProductEntity> findBySku(String sku);
}
