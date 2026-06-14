package com.healthconnector.app.controller;

import com.healthconnector.app.dto.response.*;
import com.healthconnector.app.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Role-specific dashboard analytics from MongoDB aggregations")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

	@Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Admin Dashboard", description = "Full platform analytics for Super Admin (MongoDB aggregations)")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> adminDashboard() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAdminDashboard()));
    }

    @GetMapping("/provider/dashboard")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Provider Dashboard", description = "Authorization statistics for the authenticated provider")
    public ResponseEntity<ApiResponse<ProviderDashboardResponse>> providerDashboard() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getProviderDashboard(userId)));
    }

    @GetMapping("/payer/dashboard")
    @PreAuthorize("hasRole('PAYER')")
    @Operation(summary = "Payer Dashboard", description = "Review statistics for the authenticated payer")
    public ResponseEntity<ApiResponse<PayerDashboardResponse>> payerDashboard() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getPayerDashboard(userId)));
    }
}
