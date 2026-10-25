package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Socket-based SMTP sender executing RFC 5321 conversation commands.
 */
@Singleton
@Requires(missingBeans = SmtpSender.class)
public class SocketSmtpSender implements SmtpSender {

    private static final Logger LOG = LoggerFactory.getLogger(SocketSmtpSender.class);
    private static final String CRLF = "\r\n";

    private final SmtpConfiguration config;

    public SocketSmtpSender(final SmtpConfiguration config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Override
    public void sendMail(
            final String from,
            final String to,
            final String subject,
            final String htmlBody
    ) {
        LOG.debug("Initiating SMTP connection to {}:{}", config.host(), config.port());
        try (Socket socket = new Socket(config.host(), config.port());
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(10_000);
            expectCode(reader, 220);

            sendCommand(writer, "EHLO localhost");
            readMultilineResponse(reader);

            if (config.username() != null && !config.username().isBlank()
                    && config.password() != null) {
                authenticate(writer, reader, config.username(), config.password());
            }

            sendCommand(writer, "MAIL FROM:<" + from + ">");
            expectCode(reader, 250);

            sendCommand(writer, "RCPT TO:<" + to + ">");
            expectCode(reader, 250);

            sendCommand(writer, "DATA");
            expectCode(reader, 354);

            writer.write("From: " + from + CRLF);
            writer.write("To: " + to + CRLF);
            writer.write("Subject: " + subject + CRLF);
            writer.write("MIME-Version: 1.0" + CRLF);
            writer.write("Content-Type: text/html; charset=UTF-8" + CRLF);
            writer.write(CRLF);
            writer.write(htmlBody);
            writer.write(CRLF + "." + CRLF);
            writer.flush();
            expectCode(reader, 250);

            sendCommand(writer, "QUIT");
            expectCode(reader, 221);
            LOG.info("SMTP transmission complete: to={}, subject={}", to, subject);

        } catch (IOException e) {
            throw new SmtpDeliveryException("Failed to transmit email over SMTP to " + to, e);
        }
    }

    private void authenticate(
            final BufferedWriter writer,
            final BufferedReader reader,
            final String user,
            final String pass
    ) throws IOException {
        sendCommand(writer, "AUTH LOGIN");
        expectCode(reader, 334);
        sendCommand(writer, Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8)));
        expectCode(reader, 334);
        sendCommand(writer, Base64.getEncoder().encodeToString(pass.getBytes(StandardCharsets.UTF_8)));
        expectCode(reader, 235);
    }

    private void sendCommand(final BufferedWriter writer, final String command) throws IOException {
        writer.write(command + CRLF);
        writer.flush();
    }

    private void expectCode(final BufferedReader reader, final int expectedCode) throws IOException {
        final String line = reader.readLine();
        if (line == null || !line.startsWith(String.valueOf(expectedCode))) {
            throw new SmtpDeliveryException(
                    "Unexpected SMTP server response. Expected " + expectedCode + ", got: " + line);
        }
    }

    private void readMultilineResponse(final BufferedReader reader) throws IOException {
        while (true) {
            final String line = reader.readLine();
            if (line == null || line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
    }
}
