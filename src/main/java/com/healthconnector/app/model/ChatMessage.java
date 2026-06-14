package com.healthconnector.app.model;

import com.healthconnector.app.constants.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bidirectional chat message between Provider and Payer on an Authorization Request.
 * Message content and attachment metadata are AES-256/GCM encrypted.
 */
@Document(collection = AppConstants.COL_CHAT_MESSAGES)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_auth_created", def = "{'authorization_id': 1, 'created_at': 1}")
})
public class ChatMessage {

    @Id
    private String id;

    @Field("authorization_id")
    private String authorizationId;

    @Field("sender_id")
    private String senderId;

    @Field("sender_name")
    private String senderName;

    @Field("sender_role")
    private String senderRole;

    @Field("receiver_id")
    private String receiverId;

    @Field("receiver_role")
    private String receiverRole;

    /** AES-256/GCM encrypted message content. */
    @Field("content")
    private String content;

    @Field("read")
    @Builder.Default
    private boolean read = false;

    @Field("read_at")
    private Instant readAt;

    /** List of attachments; filename and path are AES encrypted. */
    @Field("attachments")
    @Builder.Default
    private List<MessageAttachment> attachments = new ArrayList<>();

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @Field("deleted")
    @Builder.Default
    private boolean deleted = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageAttachment {
        /** AES-256/GCM encrypted. */
        private String fileName;
        /** AES-256/GCM encrypted. */
        private String filePath;
        private String contentType;
        private Long sizeBytes;
    }
}
