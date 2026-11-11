package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Driver;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailtrapEmailApiClient mailtrapEmailApiClient;
    private final String smtpHost;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromAddress;
    private final String baseUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        MailtrapEmailApiClient mailtrapEmailApiClient,
                        @Value("${spring.mail.host:}") String smtpHost,
                        @Value("${spring.mail.username:}") String smtpUsername,
                        @Value("${spring.mail.password:}") String smtpPassword,
                        @Value("${app.mail.from:no-reply@serendibride.local}") String fromAddress,
                        @Value("${app.payment.base-url:http://localhost:8081}") String baseUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailtrapEmailApiClient = mailtrapEmailApiClient;
        this.smtpHost = smtpHost;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public DeliveryResult sendWelcomeEmail(String to, String name) {
        String html = layout("""
                <h1>Welcome to Serendib Ride</h1>
                <p>Dear %s,</p>
                <p>Welcome to Taxi &amp; Cab Booking System. We are glad to have you with us.</p>
                <p>Thank you,<br>Serendib Ride Team</p>
                """.formatted(escape(name)));
        String text = "Dear " + name + ",\n\nWelcome to Serendib Ride.\n\nThank you,\nSerendib Ride Team";
        return send(to, "Welcome to Serendib Ride", text, html);
    }

    public DeliveryResult sendTestEmail(String to, String subject, String message) {
        if (subject == null || subject.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Email subject and message are required.");
        }
        String html = layout("<h1>" + escape(subject) + "</h1><p>" + escape(message).replace("\n", "<br>") + "</p>");
        return send(to, subject.trim(), message, html);
    }

    public DeliveryResult sendPasswordResetEmail(Driver driver, String token) {
        String resetUrl = baseUrl + "/driver/reset-password?token=" + token;
        String html = layout("""
                <h1>Reset your driver password</h1>
                <p>Dear %s,</p>
                <p>We received a request to reset your driver account password.</p>
                <p><a href="%s" style="display:inline-block;background:#175cd3;color:#fff;text-decoration:none;padding:12px 20px;border-radius:8px">Reset password</a></p>
                <p>This link expires in 30 minutes and can only be used once. If you did not request this, ignore this email.</p>
                """.formatted(escape(driver.getName()), escape(resetUrl)));
        String text = "Dear " + driver.getName() + ",\n\nReset your password using this one-time link:\n"
                + resetUrl + "\n\nThis link expires in 30 minutes.";
        return send(driver.getEmail(), "Reset your driver password", text, html);
    }

    private DeliveryResult send(String to, String subject, String text, String html) {
        validateEmail(to);
        JavaMailSender mailSender = smtpHost == null || smtpHost.isBlank()
                || smtpUsername == null || smtpUsername.isBlank()
                || smtpPassword == null || smtpPassword.isBlank()
                ? null : mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            try {
                var message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
                helper.setFrom(fromAddress, "Serendib Ride");
                helper.setTo(to.trim());
                helper.setSubject(subject);
                helper.setText(text, html);
                mailSender.send(message);
                return DeliveryResult.SENT;
            } catch (MailException | MessagingException | java.io.UnsupportedEncodingException exception) {
                logger.warn("SMTP email delivery failed; trying configured Mailtrap API fallback.");
            }
        }
        if (mailtrapEmailApiClient.isConfigured()) {
            return mailtrapEmailApiClient.send(to.trim(), subject, text, html)
                    ? DeliveryResult.SENT : DeliveryResult.FAILED;
        }
        return mailSender == null ? DeliveryResult.NOT_CONFIGURED : DeliveryResult.FAILED;
    }

    private void validateEmail(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("A recipient email address is required.");
        }
        try {
            InternetAddress parsed = new InternetAddress(address.trim(), true);
            parsed.validate();
            if (parsed.getAddress().length() > 254) {
                throw new IllegalArgumentException("The recipient email address is invalid.");
            }
        } catch (jakarta.mail.internet.AddressException exception) {
            throw new IllegalArgumentException("The recipient email address is invalid.");
        }
    }

    private String layout(String content) {
        return """
                <!doctype html>
                <html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                <body style="margin:0;background:#f2f4f7;font-family:Arial,sans-serif;color:#101828">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 12px">
                <tr><td align="center"><table role="presentation" width="600" cellspacing="0" cellpadding="0"
                style="width:100%%;max-width:600px;background:#fff;border-radius:16px;padding:32px">
                <tr><td><div style="font-size:13px;font-weight:bold;letter-spacing:2px;color:#2563eb">SERENDIB RIDE</div>
                %s
                </td></tr></table></td></tr></table></body></html>
                """.formatted(content);
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    public enum DeliveryResult {
        SENT,
        NOT_CONFIGURED,
        FAILED
    }
}
