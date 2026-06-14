package com.healthconnector.app.service;

import com.healthconnector.app.constants.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Email service using Spring Mail with HTML templates.
 * All sends are asynchronous to avoid blocking request threads.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.from-name}")
    private String fromName;

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName, String tempPassword) {
        String content = buildHtmlEmail(
                "Welcome to HealthConnector Platform",
                "Dear " + firstName + ",",
                "Your account has been created on the <strong>HealthConnector Prior Authorization Platform</strong>.",
                "<strong>Your temporary credentials:</strong><br/>" +
                "Email: <code>" + toEmail + "</code><br/>" +
                "Temporary Password: <code>" + tempPassword + "</code>",
                "Please log in and change your password immediately on first login.",
                "#2563eb"
        );
        send(toEmail, AppConstants.EMAIL_SUBJECT_WELCOME, content);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String newPassword) {
        String content = buildHtmlEmail(
                "Password Reset",
                "Dear " + firstName + ",",
                "Your password has been reset by the administrator.",
                "<strong>New temporary password:</strong> <code>" + newPassword + "</code>",
                "Please log in and change your password immediately.",
                "#dc2626"
        );
        send(toEmail, AppConstants.EMAIL_SUBJECT_PASSWORD_RESET, content);
    }

    @Async
    public void sendLoginAlertEmail(String toEmail, String firstName, String ipAddress, String device) {
        String content = buildHtmlEmail(
                "New Login Detected",
                "Dear " + firstName + ",",
                "A new login was detected on your account.",
                "IP Address: <code>" + ipAddress + "</code><br/>Device: <code>" + device + "</code>",
                "If this wasn't you, please contact the administrator immediately.",
                "#d97706"
        );
        send(toEmail, AppConstants.EMAIL_SUBJECT_LOGIN_ALERT, content);
    }

    @Async
    public void sendAuthorizationStatusEmail(String toEmail, String firstName,
                                              String referenceNumber, String status, String notes) {
        String subject = "Authorization " + referenceNumber + " — " + status;
        String color = switch (status) {
            case "APPROVED"           -> "#16a34a";
            case "REJECTED"           -> "#dc2626";
            case "MORE_INFO_REQUIRED" -> "#d97706";
            default                   -> "#2563eb";
        };
        String notesHtml = (notes != null && !notes.isBlank())
                ? "<br/><strong>Notes:</strong> " + notes : "";
        String content = buildHtmlEmail(
                "Authorization Status Update",
                "Dear " + firstName + ",",
                "Your prior authorization request <strong>" + referenceNumber + "</strong> has been updated.",
                "Status: <strong style='color:" + color + "'>" + status + "</strong>" + notesHtml,
                "Log in to the platform for full details.",
                color
        );
        send(toEmail, subject, content);
    }

    @Async
    public void sendPasswordResetLinkEmail(String toEmail, String firstName, String resetLink) {
        String content = buildHtmlEmail(
                "Reset Your Password",
                "Dear " + firstName + ",",
                "We received a request to reset your HealthConnector account password.",
                "<a href='" + resetLink + "' style='display:inline-block;background:#2563eb;color:#fff;" +
                "padding:12px 28px;border-radius:8px;font-weight:bold;text-decoration:none;font-size:15px;'>" +
                "Reset Password</a><br/><br/>" +
                "<small style='color:#6b7280;'>Or copy this link into your browser:<br/>" +
                "<code style='font-size:12px;word-break:break-all;'>" + resetLink + "</code></small>",
                "This link expires in 60 minutes. If you did not request a password reset, ignore this email.",
                "#dc2626"
        );
        send(toEmail, "HealthConnector — Password Reset Request", content);
    }

    @Async
    public void sendAiReviewCompletedEmail(String toEmail, String firstName,
                                            String referenceNumber, Double score, String riskLevel) {
        String content = buildHtmlEmail(
                "AI Review Completed",
                "Dear " + firstName + ",",
                "AI review for authorization <strong>" + referenceNumber + "</strong> is complete.",
                "Score: <strong>" + score + "/100</strong><br/>Risk: <strong>" + riskLevel + "</strong>",
                "You may now review the suggestions and submit your request.",
                "#7c3aed"
        );
        send(toEmail, AppConstants.EMAIL_SUBJECT_AI_REVIEW, content);
    }

    private void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent | to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email | to={} subject={} error={}", to, subject, e.getMessage(), e);
        }
    }

    private String buildHtmlEmail(String title, String greeting, String intro,
                                   String details, String footer, String accentColor) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f3f4f6;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 20px;">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.08);">
                        <tr><td style="background:%s;padding:28px 32px;">
                          <h1 style="margin:0;color:#fff;font-size:22px;">&#127973; HealthConnector Platform</h1>
                          <p style="margin:8px 0 0;color:rgba(255,255,255,.85);font-size:14px;">%s</p>
                        </td></tr>
                        <tr><td style="padding:32px;">
                          <p style="margin:0 0 16px;font-size:16px;color:#374151;">%s</p>
                          <p style="margin:0 0 16px;font-size:15px;color:#374151;">%s</p>
                          <div style="background:#f9fafb;border-left:4px solid %s;padding:16px 20px;border-radius:6px;margin:16px 0;">
                            <p style="margin:0;font-size:14px;color:#374151;">%s</p>
                          </div>
                          <p style="margin:16px 0 0;font-size:13px;color:#6b7280;">%s</p>
                        </td></tr>
                        <tr><td style="background:#f9fafb;padding:20px 32px;border-top:1px solid #e5e7eb;">
                          <p style="margin:0;font-size:12px;color:#9ca3af;text-align:center;">
                            &copy; 2026 HealthConnector Inc. &mdash; Automated message, do not reply.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(accentColor, title, greeting, intro, accentColor, details, footer);
    }
}
