package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayerDashboardResponse {
    private long pendingReviewCount;
    private long underReviewCount;
    private long approvedCount;
    private long rejectedCount;
    private long moreInfoRequiredCount;
    private long totalReviewedCount;
    private Double approvalPercentage;
    private Double rejectionPercentage;
    private Double averageReviewTimeHours;
    private List<Map<String, Object>> dailyReviews;
    private List<Map<String, Object>> providerRanking;
    private List<Map<String, Object>> monthlyTrend;
    private List<Map<String, Object>> monthlyBreakdown;
}
