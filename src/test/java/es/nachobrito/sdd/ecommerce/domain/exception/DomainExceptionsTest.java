package es.nachobrito.sdd.ecommerce.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@DisplayName("Domain Exceptions Unit Tests")
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class DomainExceptionsTest {

    @Test
    @DisplayName("OrderNotFoundException should preserve order ID and provide descriptive message")
    void shouldCreateOrderNotFoundException() {
        final UUID orderId = UUID.randomUUID();
        final OrderNotFoundException ex = new OrderNotFoundException(orderId);

        assertEquals(orderId, ex.getOrderId(), "Order ID must match");
        assertTrue(ex.getMessage().contains(orderId.toString()), "Message must contain orderId");
    }

    @Test
    @DisplayName("OrderAlreadyExistsException should preserve order ID and provide descriptive message")
    void shouldCreateOrderAlreadyExistsException() {
        final UUID orderId = UUID.randomUUID();
        final OrderAlreadyExistsException ex = new OrderAlreadyExistsException(orderId);

        assertEquals(orderId, ex.getOrderId(), "Order ID must match");
        assertTrue(ex.getMessage().contains(orderId.toString()), "Message must contain orderId");
    }

    @Test
    @DisplayName("ProductAlreadyExistsException should preserve product ID and provide descriptive message")
    void shouldCreateProductAlreadyExistsException() {
        final UUID productId = UUID.randomUUID();
        final ProductAlreadyExistsException ex = new ProductAlreadyExistsException(productId);

        assertEquals(productId, ex.getProductId(), "Product ID must match");
        assertTrue(ex.getMessage().contains(productId.toString()), "Message must contain productId");
    }
}
