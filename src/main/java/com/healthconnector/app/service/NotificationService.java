package com.healthconnector.app.service;

import com.healthconnector.app.constants.NotificationType;
import com.healthconnector.app.dto.response.NotificationResponse;
import com.healthconnector.app.exception.ForbiddenException;
import com.healthconnector.app.exception.ResourceNotFoundException;
import com.healthconnector.app.model.Notification;
import com.healthconnector.app.model.User;
import com.healthconnector.app.repository.NotificationRepository;
import com.healthconnector.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Notification service — creates DB notification records and triggers email dispatch.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Async
    public void sendAccountCreatedNotification(User user, String tempPassword) {
        createAndSave(user, NotificationType.ACCOUNT_CREATED,
                "Account Created",
                "Your account has been created. Use the temporary password to log in.",
                null, null);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), tempPassword);
    }

    @Async
    public void sendPasswordResetNotification(User user, String newPassword) {
        createAndSave(user, NotificationType.PASSWORD_RESET,
                "Password Reset",
                "Your password has been reset by the administrator.",
                null, null);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), newPassword);
    }

    @Async
    public void sendLoginAlertNotification(User user, String ipAddress, String device) {
        createAndSave(user, NotificationType.LOGIN_ALERT,
                "New Login Detected",
                "A new login was detected from " + ipAddress,
                null, null);
        emailService.sendLoginAlertEmail(user.getEmail(), user.getFirstName(), ipAddress, device);
    }

    @Async
    public void sendAuthorizationNotification(String userId, NotificationType type,
                                               String referenceNumber, String status,
                                               String authorizationId, String notes) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for notification, skipping | userId={}", userId);
            return;
        }
        String title   = "Authorization " + referenceNumber + " — " + status;
        String message = buildAuthMessage(type, referenceNumber, notes);
        createAndSave(user, type, title, message, authorizationId, "AUTHORIZATION");
        emailService.sendAuthorizationStatusEmail(user.getEmail(), user.getFirstName(), referenceNumber, status, notes);
    }

    @Async
    public void sendAiReviewNotification(String userId, String referenceNumber,
                                          String authorizationId, Double score, String riskLevel) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElse(null);
        if (user == null) return;
        createAndSave(user, NotificationType.AI_REVIEW_COMPLETED,
                "AI Review Completed — " + referenceNumber,
                "AI review completed. Score: " + score + "/100, Risk: " + riskLevel,
                authorizationId, "AUTHORIZATION");
        emailService.sendAiReviewCompletedEmail(user.getEmail(), user.getFirstName(), referenceNumber, score, riskLevel);
    }

    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Cannot access this notification");
        }
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
        log.debug("Notification marked as read | notificationId={} userId={}", notificationId, userId);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, Pageable.unpaged())
                .forEach(n -> {
                    n.setRead(true);
                    n.setReadAt(Instant.now());
                    notificationRepository.save(n);
                });
        log.info("All notifications marked as read for userId={}", userId);
    }

    private void createAndSave(User user, NotificationType type, String title,
                                String message, String entityId, String entityType) {
        Notification notification = Notification.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .type(type)
                .title(title)
                .message(message)
                .entityId(entityId)
                .entityType(entityType)
                .build();
        notificationRepository.save(notification);
        log.debug("Notification saved | type={} userId={}", type, user.getId());
    }

    private String buildAuthMessage(NotificationType type, String refNum, String notes) {
        return switch (type) {
            case AUTHORIZATION_SUBMITTED -> "Authorization " + refNum + " has been submitted for review.";
            case AUTHORIZATION_APPROVED  -> "Authorization " + refNum + " has been approved." + (notes != null ? " Notes: " + notes : "");
            case AUTHORIZATION_REJECTED  -> "Authorization " + refNum + " has been rejected." + (notes != null ? " Reason: " + notes : "");
            case MORE_INFO_REQUIRED      -> "Additional information required for " + refNum + "." + (notes != null ? " Details: " + notes : "");
            default                      -> "Authorization " + refNum + " status updated.";
        };
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .entityId(n.getEntityId())
                .entityType(n.getEntityType())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
