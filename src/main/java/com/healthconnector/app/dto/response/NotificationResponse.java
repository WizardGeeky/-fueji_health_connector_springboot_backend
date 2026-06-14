package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthconnector.app.constants.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private String entityId;
    private String entityType;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;
}
