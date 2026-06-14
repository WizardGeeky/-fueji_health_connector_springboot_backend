package com.healthconnector.app.service;

import com.healthconnector.app.constants.*;
import com.healthconnector.app.dto.request.CreateUserRequest;
import com.healthconnector.app.dto.response.UserResponse;
import com.healthconnector.app.exception.*;
import com.healthconnector.app.model.User;
import com.healthconnector.app.repository.UserRepository;
import com.healthconnector.app.utils.PasswordUtil;
import com.healthconnector.app.utils.TemporaryPasswordGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * User management service for Super Admin operations.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private TemporaryPasswordGenerator tempPasswordGenerator;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request, HttpServletRequest httpRequest) {
        String actorId    = getAuthenticatedUserId();
        String actorEmail = getAuthenticatedUserEmail();

        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new DuplicateResourceException("email", request.getEmail());
        }
        if (userRepository.existsByMobileAndDeletedFalse(request.getMobile())) {
            throw new DuplicateResourceException("mobile", request.getMobile());
        }
        if (request.getRole() == UserRole.SUPER_ADMIN) {
            throw new BusinessException("Super Admin cannot be created via this endpoint");
        }

        String tempPassword = tempPasswordGenerator.generate();
        String orgId        = UUID.randomUUID().toString();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim())
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(tempPassword))
                .role(request.getRole())
                .organizationId(orgId)
                .organizationName(request.getOrganizationName())
                .npi(request.getNpi())
                .address(request.getAddress())
                .status(UserStatus.PENDING)
                .passwordChanged(false)
                .createdBy(actorId)
                .build();

        User saved = userRepository.save(user);
        saved.setLastAdminPassword(tempPassword);
        userRepository.save(saved);
        log.info("User created | role={} email={} orgId={}", request.getRole(), request.getEmail(), orgId);

        auditService.log(actorId, actorEmail, UserRole.SUPER_ADMIN.name(),
                AuditAction.USER_CREATED, "USER", saved.getId(),
                "Created " + request.getRole() + ": " + request.getEmail(), httpRequest);

        notificationService.sendAccountCreatedNotification(saved, tempPassword);
        return toResponse(saved);
    }

    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRoleAndDeletedFalse(role, pageable).map(this::toResponse);
    }

    public UserResponse getUserById(String id) {
        return toResponse(findUserOrThrow(id));
    }

    @Transactional
    public UserResponse updateUser(String id, com.healthconnector.app.dto.request.UpdateUserRequest request, HttpServletRequest httpRequest) {
        String actorId = getAuthenticatedUserId();
        User user = findUserOrThrow(id);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobile(request.getMobile());
        user.setOrganizationName(request.getOrganizationName());
        if (request.getNpi() != null)     user.setNpi(request.getNpi());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        userRepository.save(user);
        log.info("User updated | userId={} email={}", id, user.getEmail());
        auditService.log(actorId, getAuthenticatedUserEmail(), UserRole.SUPER_ADMIN.name(),
                AuditAction.USER_UPDATED, "USER", id, "Updated: " + user.getEmail(), httpRequest);
        return toResponse(user);
    }

    @Transactional
    public UserResponse blockUser(String id, HttpServletRequest httpRequest) {
        String actorId = getAuthenticatedUserId();
        User user = findUserOrThrow(id);
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new BusinessException("Super Admin cannot be blocked");
        }
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        log.info("User blocked | userId={} email={}", id, user.getEmail());
        auditService.log(actorId, getAuthenticatedUserEmail(), UserRole.SUPER_ADMIN.name(),
                AuditAction.USER_BLOCKED, "USER", id, "Blocked: " + user.getEmail(), httpRequest);
        return toResponse(user);
    }

    @Transactional
    public UserResponse activateUser(String id, HttpServletRequest httpRequest) {
        String actorId = getAuthenticatedUserId();
        User user = findUserOrThrow(id);
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("User activated | userId={} email={}", id, user.getEmail());
        auditService.log(actorId, getAuthenticatedUserEmail(), UserRole.SUPER_ADMIN.name(),
                AuditAction.USER_ACTIVATED, "USER", id, "Activated: " + user.getEmail(), httpRequest);
        return toResponse(user);
    }

    @Transactional
    public String resetPassword(String id, HttpServletRequest httpRequest) {
        String actorId = getAuthenticatedUserId();
        User user = findUserOrThrow(id);
        String newPassword = tempPasswordGenerator.generate();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastAdminPassword(newPassword);
        user.setPasswordChanged(false);
        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("Password reset | userId={} email={}", id, user.getEmail());
        auditService.log(actorId, getAuthenticatedUserEmail(), UserRole.SUPER_ADMIN.name(),
                AuditAction.PASSWORD_RESET, "USER", id, "Password reset: " + user.getEmail(), httpRequest);
        notificationService.sendPasswordResetNotification(user, newPassword);
        return newPassword;
    }

    @Transactional
    public void softDeleteUser(String id, HttpServletRequest httpRequest) {
        String actorId = getAuthenticatedUserId();
        User user = findUserOrThrow(id);
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new BusinessException("Super Admin cannot be deleted");
        }
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        log.info("User soft deleted | userId={} email={}", id, user.getEmail());
        auditService.log(actorId, getAuthenticatedUserEmail(), UserRole.SUPER_ADMIN.name(),
                AuditAction.USER_DELETED, "USER", id, "Deleted: " + user.getEmail(), httpRequest);
    }

    private User findUserOrThrow(String id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private String getAuthenticatedUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String getAuthenticatedUserEmail() {
        try {
            return userRepository.findByIdAndDeletedFalse(getAuthenticatedUserId())
                    .map(User::getEmail).orElse(AppConstants.SYSTEM_USER);
        } catch (Exception e) {
            return AppConstants.SYSTEM_USER;
        }
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .role(user.getRole())
                .organizationId(user.getOrganizationId())
                .organizationName(user.getOrganizationName())
                .npi(user.getNpi())
                .address(user.getAddress())
                .status(user.getStatus())
                .accountLocked(user.isAccountLocked())
                .passwordChanged(user.isPasswordChanged())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .lastAdminPassword(user.getLastAdminPassword())
                .build();
    }
}
