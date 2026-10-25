package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import java.io.Serial;

/**
 * Exception thrown when an error occurs during SMTP connection or transmission.
 */
public class SmtpDeliveryException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SmtpDeliveryException(final String message) {
        super(message);
    }

    public SmtpDeliveryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
