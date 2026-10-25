package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

/**
 * Compile-time Micronaut Data JDBC repository for order_lines table.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface MicronautOrderLineRepository extends CrudRepository<OrderLineEntity, UUID> {

    /**
     * Finds all order line entities belonging to a specific order.
     *
     * @param orderId Order unique identifier
     * @return List of order line entities
     */
    List<OrderLineEntity> findByOrderId(UUID orderId);

    /**
     * Deletes all order line entities belonging to a specific order.
     *
     * @param orderId Order unique identifier
     */
    void deleteByOrderId(UUID orderId);
}
