package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Super Admin dashboard analytics response built from MongoDB aggregations.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDashboardResponse {

    // ── User counts ──────────────────────────────────────────────────
    private long totalProviders;
    private long totalPayers;
    private long activeUsers;
    private long blockedUsers;
    private long pendingUsers;

    // ── Authorization counts ─────────────────────────────────────────
    private long totalRequests;
    private long pendingRequests;
    private long approvedRequests;
    private long rejectedRequests;
    private long draftRequests;
    private long underReviewRequests;

    // ── Performance metrics ──────────────────────────────────────────
    private Double averageApprovalTimeHours;
    private Double aiAccuracyPercentage;
    private long aiUsageCount;

    // ── Distributions ────────────────────────────────────────────────
    private List<Map<String, Object>> statusDistribution;
    private List<Map<String, Object>> priorityDistribution;
    private List<Map<String, Object>> monthlyRequestTrend;
    private List<Map<String, Object>> dailyRequestTrend;
    private List<Map<String, Object>> loginTrend;
    private List<Map<String, Object>> organizationRanking;
    private List<Map<String, Object>> topProviders;
    private List<Map<String, Object>> topPayers;
}
