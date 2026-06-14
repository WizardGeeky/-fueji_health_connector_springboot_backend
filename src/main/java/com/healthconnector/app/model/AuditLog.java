package com.healthconnector.app.model;

import com.healthconnector.app.constants.AppConstants;
import com.healthconnector.app.constants.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Immutable audit log record. Once written, audit records are never updated or deleted.
 * Tracks every significant action in the system for compliance and forensics.
 */
@Document(collection = AppConstants.COL_AUDIT_LOGS)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_user_action", def = "{'user_id': 1, 'action': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_entity",      def = "{'entity_type': 1, 'entity_id': 1, 'timestamp': -1}")
})
public class AuditLog {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("user_email")
    private String userEmail;

    @Field("user_role")
    private String userRole;

    @Field("action")
    private AuditAction action;

    @Field("entity_type")
    private String entityType;

    @Indexed
    @Field("entity_id")
    private String entityId;

    @Field("old_value")
    private String oldValue;

    @Field("new_value")
    private String newValue;

    @Field("description")
    private String description;

    @Field("ip_address")
    private String ipAddress;

    @Field("user_agent")
    private String userAgent;

    @Field("device")
    private String device;

    @Field("session_id")
    private String sessionId;

    @Indexed
    @Field("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Field("success")
    @Builder.Default
    private boolean success = true;
}
