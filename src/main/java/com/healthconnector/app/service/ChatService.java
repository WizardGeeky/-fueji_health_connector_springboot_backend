package com.healthconnector.app.service;

import com.healthconnector.app.constants.*;
import com.healthconnector.app.dto.request.SendMessageRequest;
import com.healthconnector.app.dto.response.ChatMessageResponse;
import com.healthconnector.app.exception.ForbiddenException;
import com.healthconnector.app.exception.ResourceNotFoundException;
import com.healthconnector.app.model.AuthorizationRequest;
import com.healthconnector.app.model.ChatMessage;
import com.healthconnector.app.model.User;
import com.healthconnector.app.repository.AuthorizationRequestRepository;
import com.healthconnector.app.repository.ChatMessageRepository;
import com.healthconnector.app.repository.UserRepository;
import com.healthconnector.app.utils.AESUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Bidirectional chat service for Provider ↔ Payer communication on authorizations.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private AuthorizationRequestRepository authorizationRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private AuditService auditService;

    @Transactional
    public ChatMessageResponse sendMessage(String authorizationId, SendMessageRequest request,
                                            HttpServletRequest httpRequest) {
        String senderId = getAuthenticatedUserId();
        User sender = userRepository.findByIdAndDeletedFalse(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));
        AuthorizationRequest auth = authorizationRequestRepository.findByIdAndDeletedFalse(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization", "id", authorizationId));

        String receiverId;
        String receiverRole;
        if (sender.getRole() == UserRole.PROVIDER) {
            if (!auth.getProviderId().equals(senderId)) {
                throw new ForbiddenException("You are not the provider for this authorization");
            }
            receiverId   = auth.getPayerId();
            receiverRole = UserRole.PAYER.name();
        } else if (sender.getRole() == UserRole.PAYER) {
            if (!auth.getPayerId().equals(senderId)) {
                throw new ForbiddenException("You are not the payer for this authorization");
            }
            receiverId   = auth.getProviderId();
            receiverRole = UserRole.PROVIDER.name();
        } else {
            throw new ForbiddenException("Only PROVIDER and PAYER can send messages");
        }

        ChatMessage message = ChatMessage.builder()
                .authorizationId(authorizationId)
                .senderId(senderId)
                .senderName(sender.getFirstName() + " " + sender.getLastName())
                .senderRole(sender.getRole().name())
                .receiverId(receiverId)
                .receiverRole(receiverRole)
                .content(aesUtil.encrypt(request.getContent()))
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        log.info("Message sent | authorizationId={} senderId={} senderRole={}", authorizationId, senderId, sender.getRole());
        auditService.log(senderId, sender.getEmail(), sender.getRole().name(),
                AuditAction.MESSAGE_SENT, "AUTHORIZATION", authorizationId,
                "Message sent on authorization " + auth.getReferenceNumber(), httpRequest);

        return toResponse(saved);
    }

    public Page<ChatMessageResponse> getMessages(String authorizationId, Pageable pageable) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = authorizationRequestRepository.findByIdAndDeletedFalse(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization", "id", authorizationId));
        if (!auth.getProviderId().equals(userId) && !auth.getPayerId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this authorization's messages");
        }
        markMessagesAsRead(authorizationId, userId);
        return chatMessageRepository
                .findByAuthorizationIdAndDeletedFalseOrderByCreatedAtAsc(authorizationId, pageable)
                .map(this::toResponse);
    }

    private void markMessagesAsRead(String authorizationId, String userId) {
        List<ChatMessage> unread = chatMessageRepository
                .findByAuthorizationIdAndReceiverIdAndReadFalseAndDeletedFalse(authorizationId, userId);
        if (!unread.isEmpty()) {
            Instant now = Instant.now();
            unread.forEach(m -> {
                m.setRead(true);
                m.setReadAt(now);
            });
            chatMessageRepository.saveAll(unread);
            log.debug("Marked {} messages as read | authorizationId={} userId={}", unread.size(), authorizationId, userId);
        }
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .authorizationId(m.getAuthorizationId())
                .senderId(m.getSenderId())
                .senderName(m.getSenderName())
                .senderRole(m.getSenderRole())
                .receiverId(m.getReceiverId())
                .content(aesUtil.decrypt(m.getContent()))
                .read(m.isRead())
                .readAt(m.getReadAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String getAuthenticatedUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
