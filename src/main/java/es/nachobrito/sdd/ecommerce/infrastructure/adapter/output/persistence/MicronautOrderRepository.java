package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.UUID;

/**
 * Compile-time Micronaut Data JDBC repository for orders table.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface MicronautOrderRepository extends CrudRepository<OrderEntity, UUID> {
}
