package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderDashboardResponse {
    private long draftCount;
    private long submittedCount;
    private long underReviewCount;
    private long approvedCount;
    private long rejectedCount;
    private long moreInfoRequiredCount;
    private long totalCount;
    private Double averageAiScore;
    private long unreadNotifications;
    private List<Map<String, Object>> monthlyTrend;
    private List<Map<String, Object>> statusDistribution;
}
