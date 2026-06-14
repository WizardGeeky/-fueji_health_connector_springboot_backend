package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {
    private String id;
    private String authorizationId;
    private String senderId;
    private String senderName;
    private String senderRole;
    private String receiverId;
    private String content;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;
}
