package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

/**
 * Low-level sender abstraction for transmitting an email over SMTP.
 */
@FunctionalInterface
public interface SmtpSender {

    /**
     * Sends an email with the specified sender, recipient, subject, and HTML content.
     *
     * @param from Sender email address
     * @param to Recipient email address
     * @param subject Message subject line
     * @param htmlBody Rendered HTML message content
     */
    void sendMail(String from, String to, String subject, String htmlBody);
}
