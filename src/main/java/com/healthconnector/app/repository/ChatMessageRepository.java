package com.healthconnector.app.repository;

import com.healthconnector.app.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findByAuthorizationIdAndDeletedFalseOrderByCreatedAtAsc(String authorizationId, Pageable pageable);

    long countByAuthorizationIdAndReceiverIdAndReadFalseAndDeletedFalse(String authorizationId, String receiverId);

    List<ChatMessage> findByAuthorizationIdAndReceiverIdAndReadFalseAndDeletedFalse(String authorizationId, String receiverId);
}
