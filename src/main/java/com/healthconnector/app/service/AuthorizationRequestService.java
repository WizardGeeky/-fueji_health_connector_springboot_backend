package com.healthconnector.app.service;

import com.healthconnector.app.constants.*;
import com.healthconnector.app.dto.request.CreateAuthorizationRequest;
import com.healthconnector.app.dto.response.AIReviewResponse;
import com.healthconnector.app.dto.response.AuthorizationResponse;
import com.healthconnector.app.exception.*;
import com.healthconnector.app.model.AuthorizationRequest;
import com.healthconnector.app.model.User;
import com.healthconnector.app.repository.AuthorizationRequestRepository;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core service managing the prior authorization request lifecycle.
 */
@Service
public class AuthorizationRequestService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationRequestService.class);
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis() % 100000);

    @Autowired
    private AuthorizationRequestRepository authorizationRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private GeminiAIService geminiAIService;

    @Autowired
    private FHIRService fhirService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public AuthorizationResponse createDraft(CreateAuthorizationRequest req, HttpServletRequest httpRequest) {
        String providerId = getAuthenticatedUserId();
        User provider = userRepository.findByIdAndDeletedFalse(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", providerId));

        User payer = userRepository.findByIdAndDeletedFalse(req.getPayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer", "id", req.getPayerId()));
        if (payer.getRole() != UserRole.PAYER) {
            throw new BusinessException("Specified payerId does not belong to a PAYER user");
        }

        String referenceNumber = generateReferenceNumber();
        AuthorizationRequest auth = AuthorizationRequest.builder()
                .referenceNumber(referenceNumber)
                .providerId(providerId)
                .providerName(provider.getFirstName() + " " + provider.getLastName())
                .payerId(payer.getId())
                .payerName(payer.getOrganizationName())
                .organizationId(provider.getOrganizationId())
                .patientName(aesUtil.encrypt(req.getPatientName()))
                .patientDateOfBirth(req.getPatientDateOfBirth())
                .patientAddress(aesUtil.encrypt(req.getPatientAddress()))
                .patientMobile(aesUtil.encrypt(req.getPatientMobile()))
                .insuranceNumber(aesUtil.encrypt(req.getInsuranceNumber()))
                .memberId(aesUtil.encrypt(req.getMemberId()))
                .primaryDiagnosisCode(req.getPrimaryDiagnosisCode())
                .diagnosisDescription(aesUtil.encrypt(req.getDiagnosisDescription()))
                .procedureCode(req.getProcedureCode())
                .procedureDescription(aesUtil.encrypt(req.getProcedureDescription()))
                .clinicalNotes(aesUtil.encrypt(req.getClinicalNotes()))
                .requestedServiceDate(req.getRequestedServiceDate())
                .priority(req.getPriority() != null ? req.getPriority() : "ROUTINE")
                .placeOfService(req.getPlaceOfService())
                .status(AuthorizationStatus.DRAFT)
                .createdBy(providerId)
                .build();

        AuthorizationRequest saved = authorizationRequestRepository.save(auth);
        log.info("Authorization draft created | ref={} providerId={}", referenceNumber, providerId);

        auditService.log(providerId, provider.getEmail(), UserRole.PROVIDER.name(),
                AuditAction.AUTHORIZATION_CREATED, "AUTHORIZATION", saved.getId(),
                "Created draft: " + referenceNumber, httpRequest);

        return toResponse(saved);
    }

    public AIReviewResponse triggerAIReview(String authorizationId, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforceProviderAccess(auth, userId);

        if (auth.getStatus() == AuthorizationStatus.SUBMITTED
                || auth.getStatus() == AuthorizationStatus.APPROVED
                || auth.getStatus() == AuthorizationStatus.REJECTED) {
            throw new BusinessException("AI review cannot be triggered for status: " + auth.getStatus());
        }

        auth.setStatus(AuthorizationStatus.AI_REVIEW);
        authorizationRequestRepository.save(auth);

        AIReviewResponse response = geminiAIService.reviewAuthorization(authorizationId, userId);

        // Reload to get the latest @Version — reviewAuthorization() saves the document internally
        auth = findAuthOrThrow(authorizationId);
        auth.setStatus(AuthorizationStatus.DRAFT);
        authorizationRequestRepository.save(auth);

        try {
            notificationService.sendAiReviewNotification(userId, auth.getReferenceNumber(),
                    authorizationId, response.getScore(), response.getRiskLevel());
        } catch (Exception e) {
            log.warn("AI review notification failed | authorizationId={} error={}", authorizationId, e.getMessage());
        }

        return response;
    }

    @Transactional
    public AuthorizationResponse submitAuthorization(String authorizationId, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforceProviderAccess(auth, userId);

        if (auth.getStatus() != AuthorizationStatus.DRAFT
                && auth.getStatus() != AuthorizationStatus.MORE_INFO_REQUIRED
                && auth.getStatus() != AuthorizationStatus.AI_REVIEW) {
            throw new BusinessException("Only DRAFT or MORE_INFO_REQUIRED can be submitted. Current: " + auth.getStatus());
        }

        fhirService.validateAuthorizationRequest(auth);

        auth.setStatus(AuthorizationStatus.SUBMITTED);
        auth.setSubmittedAt(Instant.now());
        AuthorizationRequest saved = authorizationRequestRepository.save(auth);
        log.info("Authorization submitted | ref={} payerId={}", auth.getReferenceNumber(), auth.getPayerId());

        auditService.log(userId, "", UserRole.PROVIDER.name(),
                AuditAction.AUTHORIZATION_SUBMITTED, "AUTHORIZATION", authorizationId,
                "Submitted: " + auth.getReferenceNumber(), httpRequest);

        // Notify payer — new request waiting for review
        notificationService.sendAuthorizationNotification(auth.getPayerId(),
                NotificationType.AUTHORIZATION_SUBMITTED, auth.getReferenceNumber(),
                "SUBMITTED", authorizationId, null);

        // Notify provider — submission confirmation
        notificationService.sendAuthorizationNotification(userId,
                NotificationType.AUTHORIZATION_SUBMITTED, auth.getReferenceNumber(),
                "SUBMITTED", authorizationId,
                "Your authorization request has been submitted and is awaiting payer review.");

        return toResponse(saved);
    }

    @Transactional
    public AuthorizationResponse provideAdditionalInfo(String authorizationId, String additionalNotes, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforceProviderAccess(auth, userId);

        if (auth.getStatus() != AuthorizationStatus.MORE_INFO_REQUIRED) {
            throw new BusinessException("Only MORE_INFO_REQUIRED requests can provide additional info. Current: " + auth.getStatus());
        }

        // Append additional notes to clinical notes
        String existing = aesUtil.decrypt(auth.getClinicalNotes());
        String updated = (existing != null ? existing : "") + "\n\n[Additional Info]: " + additionalNotes.trim();
        auth.setClinicalNotes(aesUtil.encrypt(updated));
        authorizationRequestRepository.save(auth);

        log.info("Additional info provided for authorization | ref={}", auth.getReferenceNumber());

        // Resubmit
        return submitAuthorization(authorizationId, httpRequest);
    }

    @Transactional
    public AuthorizationResponse startReview(String authorizationId, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforcePayerAccess(auth, userId);

        if (auth.getStatus() != AuthorizationStatus.SUBMITTED) {
            throw new BusinessException("Can only start review for SUBMITTED requests. Current: " + auth.getStatus());
        }

        auth.setStatus(AuthorizationStatus.UNDER_REVIEW);
        auth.setReviewedAt(Instant.now());
        AuthorizationRequest saved = authorizationRequestRepository.save(auth);
        log.info("Authorization review started | ref={}", auth.getReferenceNumber());

        auditService.log(userId, "", UserRole.PAYER.name(),
                AuditAction.AUTHORIZATION_UPDATED, "AUTHORIZATION", authorizationId,
                "Review started: " + auth.getReferenceNumber(), httpRequest);

        notificationService.sendAuthorizationNotification(auth.getProviderId(),
                NotificationType.AUTHORIZATION_SUBMITTED, auth.getReferenceNumber(),
                "UNDER_REVIEW", authorizationId, "Your request is now under active review by the payer.");

        return toResponse(saved);
    }

    @Transactional
    public AuthorizationResponse reconsiderAuthorization(String authorizationId, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforcePayerAccess(auth, userId);

        if (auth.getStatus() != AuthorizationStatus.APPROVED && auth.getStatus() != AuthorizationStatus.REJECTED) {
            throw new BusinessException("Can only reconsider APPROVED or REJECTED requests. Current: " + auth.getStatus());
        }

        auth.setStatus(AuthorizationStatus.UNDER_REVIEW);
        auth.setReviewedAt(Instant.now());
        AuthorizationRequest saved = authorizationRequestRepository.save(auth);
        log.info("Authorization reconsidered | ref={}", auth.getReferenceNumber());

        auditService.log(userId, "", UserRole.PAYER.name(),
                AuditAction.AUTHORIZATION_UPDATED, "AUTHORIZATION", authorizationId,
                "Reconsidered: " + auth.getReferenceNumber(), httpRequest);

        notificationService.sendAuthorizationNotification(auth.getProviderId(),
                NotificationType.AUTHORIZATION_SUBMITTED, auth.getReferenceNumber(),
                "UNDER_REVIEW", authorizationId, "Your authorization request is being reconsidered by the payer.");

        return toResponse(saved);
    }

    public AIReviewResponse triggerPayerAIReview(String authorizationId, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforcePayerAccess(auth, userId);

        AIReviewResponse response = geminiAIService.reviewAuthorization(authorizationId, userId);

        auditService.log(userId, "", UserRole.PAYER.name(),
                AuditAction.AI_REVIEW_TRIGGERED, "AUTHORIZATION", authorizationId,
                "Payer AI review: " + auth.getReferenceNumber(), httpRequest);

        return response;
    }

    @Transactional
    public AuthorizationResponse approveAuthorization(String authorizationId, String notes, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforcePayerAccess(auth, userId);

        if (auth.getStatus() != AuthorizationStatus.SUBMITTED
                && auth.getStatus() != AuthorizationStatus.UNDER_REVIEW
                && auth.getStatus() != AuthorizationStatus.MORE_INFO_REQUIRED) {
            throw new BusinessException("Cannot approve from status: " + auth.getStatus());
        }

        auth.setStatus(AuthorizationStatus.APPROVED);
        auth.setPayerNotes(notes);
        auth.setApprovedAt(Instant.now());
        auth.setReviewedAt(Instant.now());
        AuthorizationRequest saved = authorizationRequestRepository.save(auth);
        log.info("Authorization approved | ref={}", auth.getReferenceNumber());

        auditService.log(userId, "", UserRole.PAYER.name(),
                AuditAction.AUTHORIZATION_APPROVED, "AUTHORIZATION", authorizationId,
                "Approved: " + auth.getReferenceNumber(), httpRequest);

        notificationService.sendAuthorizationNotification(auth.getProviderId(),
                NotificationType.AUTHORIZATION_APPROVED, auth.getReferenceNumber(), "APPROVED", authorizationId, notes);

        return toResponse(saved);
    }

    @Transactional
    public AuthorizationResponse rejectAuthorization(String authorizationId, String reason, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforcePayerAccess(auth, userId);

        auth.setStatus(AuthorizationStatus.REJECTED);
        auth.setRejectionReason(reason);
        auth.setRejectedAt(Instant.now());
        auth.setReviewedAt(Instant.now());
        AuthorizationRequest saved = authorizationRequestRepository.save(auth);
        log.info("Authorization rejected | ref={}", auth.getReferenceNumber());

        auditService.log(userId, "", UserRole.PAYER.name(),
                AuditAction.AUTHORIZATION_REJECTED, "AUTHORIZATION", authorizationId,
                "Rejected: " + auth.getReferenceNumber(), httpRequest);

        notificationService.sendAuthorizationNotification(auth.getProviderId(),
                NotificationType.AUTHORIZATION_REJECTED, auth.getReferenceNumber(), "REJECTED", authorizationId, reason);

        return toResponse(saved);
    }

    @Transactional
    public AuthorizationResponse requestMoreInfo(String authorizationId, String notes, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        enforcePayerAccess(auth, userId);

        auth.setStatus(AuthorizationStatus.MORE_INFO_REQUIRED);
        auth.setMoreInfoNotes(notes);
        AuthorizationRequest saved = authorizationRequestRepository.save(auth);

        notificationService.sendAuthorizationNotification(auth.getProviderId(),
                NotificationType.MORE_INFO_REQUIRED, auth.getReferenceNumber(), "MORE_INFO_REQUIRED", authorizationId, notes);

        return toResponse(saved);
    }

    public Page<AuthorizationResponse> getMyAuthorizations(Pageable pageable) {
        String userId = getAuthenticatedUserId();
        return authorizationRequestRepository.findByProviderIdAndDeletedFalse(userId, pageable).map(this::toResponse);
    }

    public Page<AuthorizationResponse> getPayerQueue(Pageable pageable) {
        String userId = getAuthenticatedUserId();
        List<AuthorizationStatus> payerStatuses = List.of(
                AuthorizationStatus.SUBMITTED, AuthorizationStatus.UNDER_REVIEW,
                AuthorizationStatus.MORE_INFO_REQUIRED, AuthorizationStatus.APPROVED,
                AuthorizationStatus.REJECTED);
        return authorizationRequestRepository.findByPayerIdAndStatusInAndDeletedFalse(userId, payerStatuses, pageable)
                .map(this::toResponse);
    }

    public Page<AuthorizationResponse> getAllAuthorizations(Pageable pageable) {
        return authorizationRequestRepository.findByDeletedFalse(pageable).map(this::toResponse);
    }

    public AuthorizationResponse getAuthorizationById(String id) {
        return toResponse(findAuthOrThrow(id));
    }

    @Transactional
    public void softDelete(String authorizationId, HttpServletRequest httpRequest) {
        String userId = getAuthenticatedUserId();
        AuthorizationRequest auth = findAuthOrThrow(authorizationId);
        if (auth.getStatus() != AuthorizationStatus.DRAFT) {
            throw new BusinessException("Only DRAFT requests can be deleted");
        }
        auth.setDeleted(true);
        auth.setDeletedAt(Instant.now());
        authorizationRequestRepository.save(auth);
        log.info("Authorization deleted | ref={}", auth.getReferenceNumber());

        auditService.log(userId, "", UserRole.PROVIDER.name(),
                AuditAction.AUTHORIZATION_DELETED, "AUTHORIZATION", authorizationId,
                "Deleted: " + auth.getReferenceNumber(), httpRequest);
    }

    private AuthorizationRequest findAuthOrThrow(String id) {
        return authorizationRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization", "id", id));
    }

    private void enforceProviderAccess(AuthorizationRequest auth, String userId) {
        if (!auth.getProviderId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this authorization request");
        }
    }

    private void enforcePayerAccess(AuthorizationRequest auth, String userId) {
        if (auth.getPayerId() == null || !auth.getPayerId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this authorization request");
        }
    }

    private String getAuthenticatedUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String generateReferenceNumber() {
        return "HA-" + System.currentTimeMillis() % 100000 + "-" + COUNTER.incrementAndGet();
    }

    public AuthorizationResponse toResponse(AuthorizationRequest a) {
        return AuthorizationResponse.builder()
                .id(a.getId())
                .referenceNumber(a.getReferenceNumber())
                .providerId(a.getProviderId())
                .providerName(a.getProviderName())
                .payerId(a.getPayerId())
                .payerName(a.getPayerName())
                .organizationId(a.getOrganizationId())
                .patientName(safeDecrypt(a.getPatientName()))
                .patientDateOfBirth(a.getPatientDateOfBirth())
                .patientAddress(safeDecrypt(a.getPatientAddress()))
                .patientMobile(safeDecrypt(a.getPatientMobile()))
                .insuranceNumber(safeDecrypt(a.getInsuranceNumber()))
                .memberId(safeDecrypt(a.getMemberId()))
                .primaryDiagnosisCode(a.getPrimaryDiagnosisCode())
                .diagnosisDescription(safeDecrypt(a.getDiagnosisDescription()))
                .procedureCode(a.getProcedureCode())
                .procedureDescription(safeDecrypt(a.getProcedureDescription()))
                .clinicalNotes(safeDecrypt(a.getClinicalNotes()))
                .requestedServiceDate(a.getRequestedServiceDate())
                .priority(a.getPriority())
                .placeOfService(a.getPlaceOfService())
                .status(a.getStatus())
                .payerNotes(a.getPayerNotes())
                .rejectionReason(a.getRejectionReason())
                .moreInfoNotes(a.getMoreInfoNotes())
                .aiScore(a.getAiScore())
                .aiRiskLevel(a.getAiRiskLevel())
                .latestAiReviewId(a.getLatestAiReviewId())
                .fhirCompliant(a.getFhirCompliant())
                .submittedAt(a.getSubmittedAt())
                .reviewedAt(a.getReviewedAt())
                .approvedAt(a.getApprovedAt())
                .rejectedAt(a.getRejectedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .createdBy(a.getCreatedBy())
                .build();
    }

    private String safeDecrypt(String value) {
        if (value == null) return null;
        try {
            return aesUtil.decrypt(value);
        } catch (Exception e) {
            log.warn("AES decrypt failed for a field, returning raw value | error={}", e.getMessage());
            return value;
        }
    }
}
