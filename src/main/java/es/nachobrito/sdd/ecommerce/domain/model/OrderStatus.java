package es.nachobrito.sdd.ecommerce.domain.model;

/**
 * Lifecycle status of an Order aggregate.
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
