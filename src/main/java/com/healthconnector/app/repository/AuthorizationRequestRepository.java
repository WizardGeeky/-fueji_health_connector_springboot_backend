package com.healthconnector.app.repository;

import com.healthconnector.app.constants.AuthorizationStatus;
import com.healthconnector.app.model.AuthorizationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link AuthorizationRequest}.
 */
@Repository
public interface AuthorizationRequestRepository extends MongoRepository<AuthorizationRequest, String> {

    Optional<AuthorizationRequest> findByIdAndDeletedFalse(String id);

    Optional<AuthorizationRequest> findByReferenceNumberAndDeletedFalse(String referenceNumber);

    boolean existsByReferenceNumberAndDeletedFalse(String referenceNumber);

    Page<AuthorizationRequest> findByProviderIdAndDeletedFalse(String providerId, Pageable pageable);

    Page<AuthorizationRequest> findByPayerIdAndDeletedFalse(String payerId, Pageable pageable);

    Page<AuthorizationRequest> findByPayerIdAndStatusInAndDeletedFalse(String payerId, List<AuthorizationStatus> statuses, Pageable pageable);

    Page<AuthorizationRequest> findByProviderIdAndStatusAndDeletedFalse(String providerId, AuthorizationStatus status, Pageable pageable);

    Page<AuthorizationRequest> findByPayerIdAndStatusAndDeletedFalse(String payerId, AuthorizationStatus status, Pageable pageable);

    Page<AuthorizationRequest> findByStatusAndDeletedFalse(AuthorizationStatus status, Pageable pageable);

    Page<AuthorizationRequest> findByDeletedFalse(Pageable pageable);

    long countByStatusAndDeletedFalse(AuthorizationStatus status);

    long countByProviderIdAndStatusAndDeletedFalse(String providerId, AuthorizationStatus status);

    long countByPayerIdAndStatusAndDeletedFalse(String payerId, AuthorizationStatus status);

    long countByDeletedFalse();

    @Query("{ 'provider_id': ?0, 'primary_diagnosis_code': ?1, 'procedure_code': ?2, 'deleted': false, 'status': { '$nin': ['REJECTED', 'COMPLETED'] } }")
    List<AuthorizationRequest> findPotentialDuplicates(String providerId, String diagnosisCode, String procedureCode);
}
