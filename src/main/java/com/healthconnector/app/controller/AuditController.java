package com.healthconnector.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthconnector.app.dto.response.ApiResponse;
import com.healthconnector.app.dto.response.AuditLogResponse;
import com.healthconnector.app.model.AuditLog;
import com.healthconnector.app.repository.AuditLogRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Immutable audit trail — SUPER_ADMIN access only")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AuditController {

	@Autowired
	private AuditLogRepository auditLogRepository;

	@GetMapping
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Get All Audit Logs", description = "Paginated audit log (SUPER_ADMIN only)")
	public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAll(@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("timestamp").descending());
		Page<AuditLogResponse> result = auditLogRepository.findAllByOrderByTimestampDesc(pageable)
				.map(this::toResponse);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Get User Audit Logs", description = "Get audit logs for a specific user")
	public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getByUser(@PathVariable("userId") String userId,
			@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("timestamp").descending());
		return ResponseEntity
				.ok(ApiResponse.success(auditLogRepository.findByUserId(userId, pageable).map(this::toResponse)));
	}

	@GetMapping("/entity/{entityType}/{entityId}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Get Entity Audit Logs", description = "Get audit logs for a specific entity")
	public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getByEntity(@PathVariable("entityType") String entityType,
			@PathVariable("entityId") String entityId, @RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("timestamp").descending());
		return ResponseEntity.ok(ApiResponse.success(
				auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(this::toResponse)));
	}

	private AuditLogResponse toResponse(AuditLog log) {
		return AuditLogResponse.builder().id(log.getId()).userId(log.getUserId()).userEmail(log.getUserEmail())
				.userRole(log.getUserRole()).action(log.getAction()).entityType(log.getEntityType())
				.entityId(log.getEntityId()).description(log.getDescription()).ipAddress(log.getIpAddress())
				.userAgent(log.getUserAgent()).success(log.isSuccess()).timestamp(log.getTimestamp()).build();
	}
}
