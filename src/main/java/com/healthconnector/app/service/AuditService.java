package com.healthconnector.app.service;

import com.healthconnector.app.constants.AuditAction;
import com.healthconnector.app.model.AuditLog;
import com.healthconnector.app.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for writing immutable audit log records.
 * All write operations are asynchronous to avoid blocking the main request thread.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async
    public void log(String userId, String userEmail, String userRole,
                    AuditAction action, String entityType, String entityId,
                    String description, HttpServletRequest request) {
        log(userId, userEmail, userRole, action, entityType, entityId,
                description, null, null, request, true);
    }

    @Async
    public void log(String userId, String userEmail, String userRole,
                    AuditAction action, String entityType, String entityId,
                    String description, String oldValue, String newValue,
                    HttpServletRequest request, boolean success) {
        try {
            String ipAddress = extractIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            String device    = resolveDevice(userAgent);

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .userRole(userRole)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .device(device)
                    .timestamp(Instant.now())
                    .success(success)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log recorded: action={} entity={}/{}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to write audit log for action={} entity={}: {}", action, entityId, e.getMessage(), e);
        }
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return "SYSTEM";
        for (String header : new String[]{"X-Forwarded-For","X-Real-IP","Proxy-Client-IP","WL-Proxy-Client-IP","HTTP_CLIENT_IP","HTTP_X_FORWARDED_FOR"}) {
            String val = request.getHeader(header);
            if (val != null && !val.isBlank() && !"unknown".equalsIgnoreCase(val)) {
                return val.split(",")[0].trim();
            }
        }
        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
        return ip;
    }

    private String resolveDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Mobile") || ua.contains("Android")) return "Mobile";
        if (ua.contains("Tablet") || ua.contains("iPad")) return "Tablet";
        return "Desktop";
    }
}
