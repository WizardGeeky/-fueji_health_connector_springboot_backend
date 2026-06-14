package com.healthconnector.app.repository;

import com.healthconnector.app.model.AIReviewHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIReviewHistoryRepository extends MongoRepository<AIReviewHistory, String> {

    Page<AIReviewHistory> findByAuthorizationIdOrderByCreatedAtDesc(String authorizationId, Pageable pageable);

    Optional<AIReviewHistory> findTopByAuthorizationIdOrderByCreatedAtDesc(String authorizationId);

    List<AIReviewHistory> findByAuthorizationId(String authorizationId);

    long countByAuthorizationId(String authorizationId);
}
