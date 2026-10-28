package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SmtpConfiguration Unit Tests")
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class SmtpConfigurationTest {

    @Test
    @DisplayName("Should apply default values when optional parameters are null, empty, blank or non-positive")
    void shouldApplyDefaults() {
        // null host, port = 0, null fromAddress
        final SmtpConfiguration config1 = new SmtpConfiguration(
                true, null, 0, null, null, null, false, false
        );
        assertEquals("localhost", config1.host(), "Host must default to localhost");
        assertEquals(1025, config1.port(), "Port must default to 1025");
        assertEquals("no-reply@ecommerce.example.com", config1.fromAddress(), "Sender must default to no-reply address");
        assertTrue(config1.enabled(), "Enabled flag must be true");

        // blank host, port = -1, blank fromAddress
        final SmtpConfiguration config2 = new SmtpConfiguration(
                false, "   ", -1, "   ", "user", "pass", true, true
        );
        assertEquals("localhost", config2.host(), "Host must default to localhost");
        assertEquals(1025, config2.port(), "Port must default to 1025");
        assertEquals("no-reply@ecommerce.example.com", config2.fromAddress(), "Sender must default to no-reply address");
        assertFalse(config2.enabled(), "Enabled flag must be false");
    }

    @Test
    @DisplayName("Should retain custom values when valid values are provided")
    void shouldRetainCustomValues() {
        final SmtpConfiguration config = new SmtpConfiguration(
                true, "smtp.example.org", 587, "orders@example.org", "api-user", "api-secret", true, false
        );
        assertEquals("smtp.example.org", config.host(), "Host must match custom value");
        assertEquals(587, config.port(), "Port must match custom value");
        assertEquals("orders@example.org", config.fromAddress(), "From address must match custom value");
        assertEquals("api-user", config.username(), "Username must match custom value");
        assertEquals("api-secret", config.password(), "Password must match custom value");
        assertTrue(config.ssl(), "SSL flag must be true");
        assertFalse(config.starttls(), "Starttls flag must be false");
    }
}
