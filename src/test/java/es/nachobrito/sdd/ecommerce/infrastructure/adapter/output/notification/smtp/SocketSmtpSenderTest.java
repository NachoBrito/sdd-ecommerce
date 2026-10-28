package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.notification.smtp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@DisplayName("SocketSmtpSender Unit Tests")
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
class SocketSmtpSenderTest {

    private static final String CRLF = "\r\n";
    private static final String DOT_TERMINATOR = ".";
    private static final String LOOPBACK_HOST = InetAddress.getLoopbackAddress().getHostAddress();

    @Test
    @DisplayName("Should successfully send email without authentication")
    void shouldSendEmailWithoutAuthentication()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        final List<String> receivedCommands = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            final int port = serverSocket.getLocalPort();
            final CompletableFuture<Void> serverFuture = CompletableFuture.runAsync(() -> {
                try (Socket client = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

                    // 1. Initial greeting
                    writer.write("220 smtp.example.com Service Ready" + CRLF);
                    writer.flush();

                    // 2. EHLO
                    receivedCommands.add(reader.readLine());
                    writer.write("250-smtp.example.com" + CRLF);
                    writer.write("250-PIPELINING" + CRLF);
                    writer.write("250 HELP" + CRLF);
                    writer.flush();

                    // 3. MAIL FROM
                    receivedCommands.add(reader.readLine());
                    writer.write("250 2.1.0 Sender OK" + CRLF);
                    writer.flush();

                    // 4. RCPT TO
                    receivedCommands.add(reader.readLine());
                    writer.write("250 2.1.5 Recipient OK" + CRLF);
                    writer.flush();

                    // 5. DATA
                    receivedCommands.add(reader.readLine());
                    writer.write("354 Start mail input; end with <CRLF>.<CRLF>" + CRLF);
                    writer.flush();

                    // Read mail content until lone dot
                    String line = reader.readLine();
                    while (line != null) {
                        receivedCommands.add(line);
                        if (DOT_TERMINATOR.equals(line)) {
                            break;
                        }
                        line = reader.readLine();
                    }
                    writer.write("250 2.0.0 OK: message queued" + CRLF);
                    writer.flush();

                    // 6. QUIT
                    receivedCommands.add(reader.readLine());
                    writer.write("221 2.0.0 Bye" + CRLF);
                    writer.flush();
                } catch (IOException e) {
                    throw new IllegalStateException("SMTP mock server error", e);
                }
            });

            final SmtpConfiguration config = new SmtpConfiguration(
                    true, LOOPBACK_HOST, port, "no-reply@ecommerce.com", null, null, false, false
            );
            final SocketSmtpSender sender = new SocketSmtpSender(config);

            assertDoesNotThrow(() -> sender.sendMail(
                    "no-reply@ecommerce.com",
                    "customer@example.com",
                    "Order #1001",
                    "<h1>Thank you for your order!</h1>"
            ), "sendMail must succeed when server returns valid SMTP codes");

            serverFuture.get(5, TimeUnit.SECONDS);

            assertEquals("EHLO localhost", receivedCommands.get(0), "EHLO command must be issued");
            assertEquals("MAIL FROM:<no-reply@ecommerce.com>", receivedCommands.get(1), "MAIL FROM command must match");
            assertEquals("RCPT TO:<customer@example.com>", receivedCommands.get(2), "RCPT TO command must match");
            assertEquals("DATA", receivedCommands.get(3), "DATA command must be issued");
            assertTrue(receivedCommands.contains("From: no-reply@ecommerce.com"), "From header must be sent");
            assertTrue(receivedCommands.contains("To: customer@example.com"), "To header must be sent");
            assertTrue(receivedCommands.contains("Subject: Order #1001"), "Subject header must be sent");
            assertTrue(receivedCommands.contains("<h1>Thank you for your order!</h1>"), "Body must be sent");
            assertEquals("QUIT", receivedCommands.get(receivedCommands.size() - 1), "QUIT must be final command");
        }
    }

    @Test
    @DisplayName("Should successfully send email with AUTH LOGIN authentication")
    void shouldSendEmailWithAuthentication()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        final List<String> receivedCommands = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            final int port = serverSocket.getLocalPort();
            final CompletableFuture<Void> serverFuture = CompletableFuture.runAsync(() -> {
                try (Socket client = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

                    writer.write("220 smtp.example.com Service Ready" + CRLF);
                    writer.flush();

                    // EHLO
                    receivedCommands.add(reader.readLine());
                    writer.write("250 HELP" + CRLF);
                    writer.flush();

                    // AUTH LOGIN
                    receivedCommands.add(reader.readLine());
                    writer.write("334 VXNlcm5hbWU6" + CRLF);
                    writer.flush();

                    // Username
                    receivedCommands.add(reader.readLine());
                    writer.write("334 UGFzc3dvcmQ6" + CRLF);
                    writer.flush();

                    // Password
                    receivedCommands.add(reader.readLine());
                    writer.write("235 2.7.0 Authentication successful" + CRLF);
                    writer.flush();

                    // MAIL FROM
                    receivedCommands.add(reader.readLine());
                    writer.write("250 OK" + CRLF);
                    writer.flush();

                    // RCPT TO
                    receivedCommands.add(reader.readLine());
                    writer.write("250 OK" + CRLF);
                    writer.flush();

                    // DATA
                    receivedCommands.add(reader.readLine());
                    writer.write("354 Start mail input" + CRLF);
                    writer.flush();

                    String line = reader.readLine();
                    while (line != null) {
                        if (DOT_TERMINATOR.equals(line)) {
                            break;
                        }
                        line = reader.readLine();
                    }
                    writer.write("250 OK" + CRLF);
                    writer.flush();

                    // QUIT
                    receivedCommands.add(reader.readLine());
                    writer.write("221 Bye" + CRLF);
                    writer.flush();
                } catch (IOException e) {
                    throw new IllegalStateException("SMTP mock server error", e);
                }
            });

            final SmtpConfiguration config = new SmtpConfiguration(
                    true, LOOPBACK_HOST, port, "no-reply@ecommerce.com", "myuser", "mypass", false, false
            );
            final SocketSmtpSender sender = new SocketSmtpSender(config);

            assertDoesNotThrow(() -> sender.sendMail(
                    "no-reply@ecommerce.com",
                    "customer@example.com",
                    "Auth Order",
                    "<p>Authenticated email body</p>"
            ), "sendMail with auth must succeed");

            serverFuture.get(5, TimeUnit.SECONDS);

            assertTrue(receivedCommands.contains("AUTH LOGIN"), "AUTH LOGIN command must be sent");
            final String encodedUser = Base64.getEncoder().encodeToString("myuser".getBytes(StandardCharsets.UTF_8));
            final String encodedPass = Base64.getEncoder().encodeToString("mypass".getBytes(StandardCharsets.UTF_8));
            assertTrue(receivedCommands.contains(encodedUser), "Base64 encoded username must be sent");
            assertTrue(receivedCommands.contains(encodedPass), "Base64 encoded password must be sent");
        }
    }

    @Test
    @DisplayName("Should throw SmtpDeliveryException when server sends unexpected response code")
    void shouldThrowExceptionWhenServerReturnsUnexpectedCode() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            final int port = serverSocket.getLocalPort();
            CompletableFuture.runAsync(() -> {
                try (Socket client = serverSocket.accept();
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
                    // Send 554 error instead of 220
                    writer.write("554 Service unavailable" + CRLF);
                    writer.flush();
                } catch (IOException ignored) {
                    // Expected during test
                }
            });

            final SmtpConfiguration config = new SmtpConfiguration(
                    true, LOOPBACK_HOST, port, "no-reply@ecommerce.com", null, null, false, false
            );
            final SocketSmtpSender sender = new SocketSmtpSender(config);

            final SmtpDeliveryException ex = assertThrows(SmtpDeliveryException.class, () -> sender.sendMail(
                    "no-reply@ecommerce.com",
                    "customer@example.com",
                    "Subject",
                    "Body"
            ), "Unexpected SMTP response code must throw SmtpDeliveryException");
            assertTrue(ex.getMessage().contains("Unexpected SMTP server response"), "Exception message must report unexpected code");
        }
    }

    @Test
    @DisplayName("Should throw SmtpDeliveryException when connection fails")
    void shouldThrowExceptionWhenConnectionFails() {
        // Port 1 is typically closed/refused on localhost
        final SmtpConfiguration config = new SmtpConfiguration(
                true, LOOPBACK_HOST, 1, "no-reply@ecommerce.com", null, null, false, false
        );
        final SocketSmtpSender sender = new SocketSmtpSender(config);

        final SmtpDeliveryException ex = assertThrows(SmtpDeliveryException.class, () -> sender.sendMail(
                "no-reply@ecommerce.com",
                "customer@example.com",
                "Subject",
                "Body"
        ), "Connection failure must throw SmtpDeliveryException");
        assertTrue(ex.getMessage().contains("Failed to transmit email"), "Exception message must report transmission failure");
    }

    @Test
    @DisplayName("Should reject null configuration in constructor")
    void shouldRejectNullConfiguration() {
        assertThrows(NullPointerException.class, () -> new SocketSmtpSender(null), "Null config must be rejected");
    }
}
