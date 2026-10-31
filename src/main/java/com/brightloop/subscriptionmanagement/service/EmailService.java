package com.brightloop.subscriptionmanagement.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
 *     → Useful when Mailtrap/SMTP is not configured or during initial development.
 *
 *   app.mail.use-console=false
 *     → Sends a real email via SMTP (Gmail, Mailtrap, Brevo, or corporate SMTP).
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.use-console:true}")
    private boolean useConsole;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username:no-reply@looppay.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends (or logs) a password reset email with a unique reset link.
     *
     * The reset link format:
     *   http://localhost:3000/reset-password?token=<uuid>
     *
     * @param toEmail   the recipient's email address
     * @param firstName the recipient's first name (used for personalization)
     * @param token     the unique reset token (UUID)
     */
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

        // Always log to console as a backup for local debugging
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📧 PASSWORD RESET REQUEST");
        log.info("   To      : {}", toEmail);
        log.info("   Name    : {}", firstName);
        log.info("   Token   : {}", token);
        log.info("   Link    : {}", resetLink);
        log.info("   Mode    : {}", useConsole ? "CONSOLE ONLY" : "SMTP EMAIL");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (!useConsole) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                String senderAddress = (fromEmail != null && !fromEmail.isBlank() && !fromEmail.equals("your_mailtrap_username"))
                        ? fromEmail
                        : "no-reply@looppay.com";

                helper.setFrom(senderAddress);
                helper.setTo(toEmail);
                helper.setSubject("LoopPay — Reset Your Password");
                helper.setText(
                        buildResetEmailBody(firstName, resetLink),
                        buildResetEmailHtml(firstName, resetLink)
                );

                mailSender.send(mimeMessage);
                log.info("✅ Password reset email successfully delivered via SMTP to: {}", toEmail);

            } catch (Exception ex) {
                log.error("❌ Failed to send password reset email via SMTP to {}: {}", toEmail, ex.getMessage());
                log.error("Please verify your spring.mail.* settings in application.properties.");
            }
        }
    }

    /**
     * Plain-text fallback for email clients that do not render HTML.
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
                """.formatted(firstName != null ? firstName : "there", resetLink);
    }

    /**
     * Rich responsive HTML email with a direct clickable button.
     */
    private String buildResetEmailHtml(String firstName, String resetLink) {
        String displayName = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Reset Your Password</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F8F9FA; margin: 0; padding: 30px 15px; color: #1F2937;">
                  <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 560px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #E5E7EB;">
                    <!-- Header -->
                    <tr>
                      <td style="background-color: #361032; padding: 28px 36px; text-align: left;">
                        <span style="font-size: 22px; font-weight: 800; color: #F5A623; letter-spacing: -0.5px;">Loop<span style="color: #ffffff;">Pay</span></span>
                        <span style="display: block; font-size: 11px; color: #D1D5DB; margin-top: 4px; text-transform: uppercase; letter-spacing: 1px;">Subscription & Billing Engine</span>
                      </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                      <td style="padding: 36px;">
                        <h2 style="font-size: 20px; font-weight: 700; color: #111827; margin: 0 0 16px;">Password Reset Request</h2>
                        <p style="font-size: 14px; line-height: 22px; color: #4B5563; margin: 0 0 20px;">
                          Hi <strong>%s</strong>,<br><br>
                          We received a request to reset the password for your LoopPay account. Click the button below to choose a new password:
                        </p>
                        <!-- Action Button -->
                        <table border="0" cellpadding="0" cellspacing="0" style="margin: 28px 0;">
                          <tr>
                            <td align="center" style="border-radius: 10px; background-color: #F5A623;">
                              <a href="%s" target="_blank" style="display: inline-block; padding: 14px 32px; font-size: 14px; font-weight: 700; color: #111827; text-decoration: none; border-radius: 10px;">
                                Reset My Password &rarr;
                              </a>
                            </td>
                          </tr>
                        </table>
                        <p style="font-size: 12px; line-height: 18px; color: #6B7280; margin: 20px 0 0;">
                          This reset link will expire in <strong>1 hour</strong>.<br>
                          If you did not request this change, you can safely ignore this email &mdash; your password will stay secure.
                        </p>
                        <hr style="border: none; border-top: 1px solid #E5E7EB; margin: 28px 0 20px;">
                        <p style="font-size: 11px; line-height: 16px; color: #9CA3AF; margin: 0;">
                          If the button doesn't work, copy and paste this URL into your browser:<br>
                          <a href="%s" style="color: #F5A623; word-break: break-all;">%s</a>
                        </p>
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="background-color: #F9FAFB; padding: 18px 36px; text-align: center; border-top: 1px solid #E5E7EB;">
                        <p style="font-size: 11px; color: #9CA3AF; margin: 0;">
                          &copy; 2026 LoopPay Inc. All rights reserved.
                        </p>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, resetLink, resetLink, resetLink);
    }
}
