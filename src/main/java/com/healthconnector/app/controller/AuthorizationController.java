package com.healthconnector.app.controller;

import com.healthconnector.app.dto.request.CreateAuthorizationRequest;
import com.healthconnector.app.dto.response.AIReviewResponse;
import com.healthconnector.app.dto.response.ApiResponse;
import com.healthconnector.app.dto.response.AuthorizationResponse;
import com.healthconnector.app.service.AuthorizationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authorizations")
@Tag(name = "Authorizations", description = "Prior Authorization lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class AuthorizationController {

    @Autowired
    private AuthorizationRequestService authorizationRequestService;

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Create Draft")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> create(
            @Valid @RequestBody CreateAuthorizationRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Draft created", authorizationRequestService.createDraft(request, httpRequest)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "My Authorizations")
    public ResponseEntity<ApiResponse<Page<AuthorizationResponse>>> getMyAuthorizations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                authorizationRequestService.getMyAuthorizations(
                        PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Payer Queue")
    public ResponseEntity<ApiResponse<Page<AuthorizationResponse>>> getPayerQueue(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                authorizationRequestService.getPayerQueue(
                        PageRequest.of(page, Math.min(size, 100), Sort.by("submittedAt").descending()))));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "All Authorizations")
    public ResponseEntity<ApiResponse<Page<AuthorizationResponse>>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                authorizationRequestService.getAllAuthorizations(
                        PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PROVIDER','PAYER')")
    @Operation(summary = "Get Authorization")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(authorizationRequestService.getAuthorizationById(id)));
    }

    @PostMapping("/{id}/ai-review")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Trigger AI Review")
    public ResponseEntity<ApiResponse<AIReviewResponse>> triggerAiReview(
            @PathVariable("id") String id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("AI review completed",
                authorizationRequestService.triggerAIReview(id, httpRequest)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Submit Authorization")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> submit(
            @PathVariable("id") String id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Submitted",
                authorizationRequestService.submitAuthorization(id, httpRequest)));
    }

    @PostMapping("/{id}/under-review")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Start Review")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> startReview(
            @PathVariable("id") String id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Review started",
                authorizationRequestService.startReview(id, httpRequest)));
    }

    @PostMapping("/{id}/reconsider")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Reconsider Authorization")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> reconsider(
            @PathVariable("id") String id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Reconsidered",
                authorizationRequestService.reconsiderAuthorization(id, httpRequest)));
    }

    @PostMapping("/{id}/ai-review/payer")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Payer AI Review")
    public ResponseEntity<ApiResponse<AIReviewResponse>> triggerPayerAiReview(
            @PathVariable("id") String id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("AI review completed",
                authorizationRequestService.triggerPayerAIReview(id, httpRequest)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Approve Authorization")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> approve(
            @PathVariable("id") String id,
            @RequestParam(name = "notes", required = false) String notes,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Approved",
                authorizationRequestService.approveAuthorization(id, notes, httpRequest)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Reject Authorization")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> reject(
            @PathVariable("id") String id,
            @RequestParam(name = "reason", required = false) String reason,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Rejected",
                authorizationRequestService.rejectAuthorization(id, reason, httpRequest)));
    }

    @PostMapping("/{id}/request-info")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Request More Info")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> requestMoreInfo(
            @PathVariable("id") String id,
            @RequestParam("notes") String notes,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("More info requested",
                authorizationRequestService.requestMoreInfo(id, notes, httpRequest)));
    }

    @PostMapping("/{id}/provide-info")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Provide Additional Info and Resubmit")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> provideInfo(
            @PathVariable("id") String id,
            @RequestParam("additionalNotes") String additionalNotes,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Resubmitted with additional info",
                authorizationRequestService.provideAdditionalInfo(id, additionalNotes, httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Delete Draft")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id, HttpServletRequest httpRequest) {
        authorizationRequestService.softDelete(id, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Authorization deleted"));
    }
}
