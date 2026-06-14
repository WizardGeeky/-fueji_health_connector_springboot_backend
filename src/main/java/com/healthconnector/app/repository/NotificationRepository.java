package com.healthconnector.app.repository;

import com.healthconnector.app.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadOrderByCreatedAtDesc(String userId, boolean read, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);
}
