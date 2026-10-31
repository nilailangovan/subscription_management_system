package com.brightloop.subscriptionmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails.
 *
 * This service handles the forgot-password email flow.
 *
 * Modes of operation (controlled by application.properties):
 *
 *   app.mail.use-console=true
 *     → Prints the reset link to the server console instead of sending an email.
 *     → Useful when Mailtrap is not configured or during initial development.
 *
 *   app.mail.use-console=false
 *     → Sends a real email via SMTP (Mailtrap in dev, real SMTP in production).
 *
 * To configure Mailtrap:
 *   1. Sign up at https://mailtrap.io (free tier is available)
 *   2. Go to: Email Testing → Inboxes → Your Inbox → SMTP Settings
 *   3. Copy the username and password into application.properties
 */
@Service
@Slf4j
public class EmailService {

    // Injected from application.properties
    private final JavaMailSender mailSender;

    @Value("${app.mail.use-console:true}")
    private boolean useConsole;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * Constructor injection of JavaMailSender.
     * Spring auto-configures this based on the spring.mail.* properties.
     */
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends (or logs) a password reset email with a unique reset link.
     *
     * The reset link format:
     *   http://your-frontend.com/reset-password?token=<uuid>
     *
     * The frontend should read the token from the URL and send it to
     * POST /api/auth/reset-password along with the new password.
     *
     * @param toEmail   the recipient's email address
     * @param firstName the recipient's first name (used for personalization)
     * @param token     the unique reset token (UUID)
     */
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        // Build the reset link
        String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

        if (useConsole) {
            // ── Console Mode: Log the reset link to the server console ──
            // This is for development when Mailtrap is not set up yet.
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📧 PASSWORD RESET EMAIL (Console Mode)");
            log.info("   To      : {}", toEmail);
            log.info("   Name    : {}", firstName);
            log.info("   Token   : {}", token);
            log.info("   Link    : {}", resetLink);
            log.info("   Expires : 1 hour from now");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        } else {
            // ── Email Mode: Send via SMTP (Mailtrap or real SMTP) ──
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject("LoopPay — Password Reset Request");
                message.setText(buildResetEmailBody(firstName, resetLink));
                message.setFrom("no-reply@looppay.com");

                mailSender.send(message);
                log.info("Password reset email sent to: {}", toEmail);

            } catch (Exception ex) {
                // If email sending fails, log the error but don't crash the request
                // The user won't know (security: don't reveal email send status)
                log.error("Failed to send password reset email to {}: {}", toEmail, ex.getMessage());
            }
        }
    }

    /**
     * Builds the plain-text body of the password reset email.
     *
     * @param firstName the recipient's name for personalization
     * @param resetLink the full reset URL
     * @return the email body as a String
     */
    private String buildResetEmailBody(String firstName, String resetLink) {
        return """
                Hi %s,
                
                We received a request to reset your LoopPay account password.
                
                Click the link below to reset your password (valid for 1 hour):
                
                %s
                
                If you did not request a password reset, please ignore this email.
                Your password will remain unchanged.
                
                Best regards,
                The LoopPay Team
                """.formatted(firstName, resetLink);
    }
}

