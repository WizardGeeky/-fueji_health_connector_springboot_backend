package com.healthconnector.app.model;

import com.healthconnector.app.constants.AppConstants;
import com.healthconnector.app.constants.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * In-app notification record. Also triggers email dispatch on creation.
 */
@Document(collection = AppConstants.COL_NOTIFICATIONS)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_user_read", def = "{'user_id': 1, 'read': 1, 'created_at': -1}")
})
public class Notification {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("user_email")
    private String userEmail;

    @Field("type")
    private NotificationType type;

    @Field("title")
    private String title;

    @Field("message")
    private String message;

    @Field("entity_id")
    private String entityId;

    @Field("entity_type")
    private String entityType;

    @Field("read")
    @Builder.Default
    private boolean read = false;

    @Field("read_at")
    private Instant readAt;

    @Field("email_sent")
    @Builder.Default
    private boolean emailSent = false;

    @Field("email_sent_at")
    private Instant emailSentAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;
}
