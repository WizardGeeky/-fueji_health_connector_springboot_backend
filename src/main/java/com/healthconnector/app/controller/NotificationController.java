package com.healthconnector.app.controller;

import com.healthconnector.app.dto.response.ApiResponse;
import com.healthconnector.app.dto.response.NotificationResponse;
import com.healthconnector.app.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notification management")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

	@Autowired
    private NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PROVIDER','PAYER')")
    @Operation(summary = "Get Notifications", description = "Get paginated notifications for the authenticated user")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUserNotifications(userId, pageable)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PROVIDER','PAYER')")
    @Operation(summary = "Unread Count", description = "Get unread notification count for the authenticated user")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PROVIDER','PAYER')")
    @Operation(summary = "Mark Read", description = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable("id") String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PROVIDER','PAYER')")
    @Operation(summary = "Mark All Read", description = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }
}
