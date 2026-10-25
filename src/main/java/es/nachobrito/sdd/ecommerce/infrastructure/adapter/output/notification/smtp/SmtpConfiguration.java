package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

/**
 * Configuration properties for the SMTP notification transport.
 *
 * @param enabled Flag indicating whether SMTP notifications are enabled
 * @param host SMTP server hostname or IP address
 * @param port SMTP port number
 * @param fromAddress Sender email address placed in the From header
 * @param username Optional username for SMTP authentication
 * @param password Optional password for SMTP authentication
 * @param ssl Whether to connect via SSL/TLS socket
 * @param starttls Whether to issue STARTTLS command
 */
@ConfigurationProperties("smtp")
public record SmtpConfiguration(
        boolean enabled,
        @NonNull String host,
        int port,
        @NonNull String fromAddress,
        @Nullable String username,
        @Nullable String password,
        boolean ssl,
        boolean starttls
) {

    public SmtpConfiguration {
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (port <= 0) {
            port = 1025;
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            fromAddress = "no-reply@ecommerce.example.com";
        }
    }
}
