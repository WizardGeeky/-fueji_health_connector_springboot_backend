package com.healthconnector.app.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthconnector.app.constants.AuditAction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {
	private String id;
	private String userId;
	private String userEmail;
	private String userRole;
	private AuditAction action;
	private String entityType;
	private String entityId;
	private String description;
	private String ipAddress;
	private String userAgent;
	private boolean success;
	private Instant timestamp;
}
