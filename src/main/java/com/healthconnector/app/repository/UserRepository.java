package com.healthconnector.app.repository;

import com.healthconnector.app.constants.UserRole;
import com.healthconnector.app.constants.UserStatus;
import com.healthconnector.app.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link User} entity. All queries automatically exclude soft-deleted records.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByMobileAndDeletedFalse(String mobile);

    Optional<User> findByIdAndDeletedFalse(String id);

    Page<User> findByRoleAndDeletedFalse(UserRole role, Pageable pageable);

    Page<User> findByRoleAndStatusAndDeletedFalse(UserRole role, UserStatus status, Pageable pageable);

    Page<User> findByDeletedFalse(Pageable pageable);

    long countByRoleAndDeletedFalse(UserRole role);

    long countByStatusAndDeletedFalse(UserStatus status);

    long countByRoleAndStatusAndDeletedFalse(UserRole role, UserStatus status);

    @Query("{ 'role': 'SUPER_ADMIN', 'deleted': false }")
    Optional<User> findFirstSuperAdmin();

    boolean existsByRoleAndDeletedFalse(UserRole role);

    Optional<User> findByPasswordResetTokenAndDeletedFalse(String token);

    @Query("{ '$or': [ { 'first_name': { '$regex': ?0, '$options': 'i' } }, { 'last_name': { '$regex': ?0, '$options': 'i' } }, { 'organization_name': { '$regex': ?0, '$options': 'i' } } ], 'role': ?1, 'deleted': false }")
    Page<User> searchByKeywordAndRole(String keyword, UserRole role, Pageable pageable);
}
