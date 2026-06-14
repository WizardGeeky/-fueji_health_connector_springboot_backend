package com.healthconnector.app.service;

import com.healthconnector.app.constants.AuthorizationStatus;
import com.healthconnector.app.constants.UserRole;
import com.healthconnector.app.constants.UserStatus;
import com.healthconnector.app.dto.response.AdminDashboardResponse;
import com.healthconnector.app.dto.response.PayerDashboardResponse;
import com.healthconnector.app.dto.response.ProviderDashboardResponse;
import com.healthconnector.app.repository.AIReviewHistoryRepository;
import com.healthconnector.app.repository.AuthorizationRequestRepository;
import com.healthconnector.app.repository.NotificationRepository;
import com.healthconnector.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * Analytics service using pure MongoDB aggregation pipelines.
 * No in-memory processing — all aggregations are performed server-side.
 * Results are mapped to {@link Document} (type-safe) then converted to Map for JSON serialization.
 */
@Service
@Slf4j
public class AnalyticsService {

	@Autowired
    private UserRepository userRepository;
	
	@Autowired
    private AuthorizationRequestRepository authorizationRequestRepository;
	
	@Autowired
    private AIReviewHistoryRepository aiReviewHistoryRepository;
	
	@Autowired
    private NotificationRepository notificationRepository;
	
	@Autowired
    private MongoTemplate mongoTemplate;

    public AdminDashboardResponse getAdminDashboard() {
        return AdminDashboardResponse.builder()
                .totalProviders(userRepository.countByRoleAndDeletedFalse(UserRole.PROVIDER))
                .totalPayers(userRepository.countByRoleAndDeletedFalse(UserRole.PAYER))
                .activeUsers(userRepository.countByStatusAndDeletedFalse(UserStatus.ACTIVE))
                .blockedUsers(userRepository.countByStatusAndDeletedFalse(UserStatus.BLOCKED))
                .totalRequests(authorizationRequestRepository.countByDeletedFalse())
                .pendingRequests(
                        authorizationRequestRepository.countByStatusAndDeletedFalse(AuthorizationStatus.SUBMITTED)
                        + authorizationRequestRepository.countByStatusAndDeletedFalse(AuthorizationStatus.UNDER_REVIEW))
                .underReviewRequests(authorizationRequestRepository.countByStatusAndDeletedFalse(AuthorizationStatus.UNDER_REVIEW))
                .approvedRequests(authorizationRequestRepository.countByStatusAndDeletedFalse(AuthorizationStatus.APPROVED))
                .rejectedRequests(authorizationRequestRepository.countByStatusAndDeletedFalse(AuthorizationStatus.REJECTED))
                .draftRequests(
                        authorizationRequestRepository.countByStatusAndDeletedFalse(AuthorizationStatus.DRAFT)
                        + authorizationRequestRepository.countByStatusAndDeletedFalse(AuthorizationStatus.AI_REVIEW))
                .aiUsageCount(aiReviewHistoryRepository.count())
                .statusDistribution(getStatusDistribution())
                .priorityDistribution(getPriorityDistribution())
                .monthlyRequestTrend(getMonthlyTrend("authorizations"))
                .organizationRanking(getOrganizationRanking())
                .topProviders(getTopUsers(UserRole.PROVIDER))
                .topPayers(getTopUsers(UserRole.PAYER))
                .build();
    }

    public ProviderDashboardResponse getProviderDashboard(String providerId) {
        return ProviderDashboardResponse.builder()
                .draftCount(authorizationRequestRepository.countByProviderIdAndStatusAndDeletedFalse(providerId, AuthorizationStatus.DRAFT))
                .submittedCount(authorizationRequestRepository.countByProviderIdAndStatusAndDeletedFalse(providerId, AuthorizationStatus.SUBMITTED))
                .underReviewCount(authorizationRequestRepository.countByProviderIdAndStatusAndDeletedFalse(providerId, AuthorizationStatus.UNDER_REVIEW))
                .approvedCount(authorizationRequestRepository.countByProviderIdAndStatusAndDeletedFalse(providerId, AuthorizationStatus.APPROVED))
                .rejectedCount(authorizationRequestRepository.countByProviderIdAndStatusAndDeletedFalse(providerId, AuthorizationStatus.REJECTED))
                .moreInfoRequiredCount(authorizationRequestRepository.countByProviderIdAndStatusAndDeletedFalse(providerId, AuthorizationStatus.MORE_INFO_REQUIRED))
                .unreadNotifications(notificationRepository.countByUserIdAndReadFalse(providerId))
                .statusDistribution(getProviderStatusDistribution(providerId))
                .monthlyTrend(getProviderMonthlyTrend(providerId))
                .build();
    }

    public PayerDashboardResponse getPayerDashboard(String payerId) {
        long approved        = authorizationRequestRepository.countByPayerIdAndStatusAndDeletedFalse(payerId, AuthorizationStatus.APPROVED);
        long rejected        = authorizationRequestRepository.countByPayerIdAndStatusAndDeletedFalse(payerId, AuthorizationStatus.REJECTED);
        long moreInfo        = authorizationRequestRepository.countByPayerIdAndStatusAndDeletedFalse(payerId, AuthorizationStatus.MORE_INFO_REQUIRED);
        long underReview     = authorizationRequestRepository.countByPayerIdAndStatusAndDeletedFalse(payerId, AuthorizationStatus.UNDER_REVIEW);
        long total           = approved + rejected;
        return PayerDashboardResponse.builder()
                .pendingReviewCount(authorizationRequestRepository.countByPayerIdAndStatusAndDeletedFalse(payerId, AuthorizationStatus.SUBMITTED))
                .underReviewCount(underReview)
                .approvedCount(approved)
                .rejectedCount(rejected)
                .moreInfoRequiredCount(moreInfo)
                .totalReviewedCount(total)
                .approvalPercentage(total > 0 ? (double) approved / total * 100 : 0.0)
                .rejectionPercentage(total > 0 ? (double) rejected / total * 100 : 0.0)
                .providerRanking(getPayerProviderRanking(payerId))
                .monthlyTrend(getPayerMonthlyTrend(payerId))
                .monthlyBreakdown(getPayerMonthlyBreakdown(payerId))
                .build();
    }

    // ── Private Aggregation Helpers ────────────────────────────────────

    private List<Map<String, Object>> getStatusDistribution() {
        Aggregation agg = newAggregation(
                match(Criteria.where("deleted").is(false)),
                group("status").count().as("count"),
                project("count").and("_id").as("status")
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getPriorityDistribution() {
        Aggregation agg = newAggregation(
                match(Criteria.where("deleted").is(false)),
                group("priority").count().as("count"),
                project("count").and("_id").as("priority")
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getMonthlyTrend(String collection) {
        Aggregation agg = newAggregation(
                match(Criteria.where("deleted").is(false)),
                project().andExpression("month(created_at)").as("month")
                         .andExpression("year(created_at)").as("year"),
                group(Fields.fields("year", "month")).count().as("count"),
                sort(Sort.Direction.ASC, "year", "month"),
                limit(12)
        );
        return toMapList(mongoTemplate.aggregate(agg, collection, Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getOrganizationRanking() {
        Aggregation agg = newAggregation(
                match(Criteria.where("deleted").is(false)),
                group("organization_id")
                        .count().as("totalRequests")
                        .first("provider_name").as("providerName"),
                sort(Sort.Direction.DESC, "totalRequests"),
                limit(10)
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getTopUsers(UserRole role) {
        Aggregation agg = newAggregation(
                match(Criteria.where("deleted").is(false).and("role").is(role.name())),
                project().and("first_name").as("firstName").and("last_name").as("lastName")
                          .and("organization_name").as("organizationName").and("last_login").as("lastLogin"),
                limit(5)
        );
        return toMapList(mongoTemplate.aggregate(agg, "users", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getProviderStatusDistribution(String providerId) {
        Aggregation agg = newAggregation(
                match(Criteria.where("provider_id").is(providerId).and("deleted").is(false)),
                group("status").count().as("count"),
                project("count").and("_id").as("status")
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getProviderMonthlyTrend(String providerId) {
        Aggregation agg = newAggregation(
                match(Criteria.where("provider_id").is(providerId).and("deleted").is(false)),
                project().andExpression("month(created_at)").as("month").andExpression("year(created_at)").as("year"),
                group(Fields.fields("year", "month")).count().as("count"),
                sort(Sort.Direction.ASC, "year", "month"),
                limit(12)
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getPayerProviderRanking(String payerId) {
        Aggregation agg = newAggregation(
                match(Criteria.where("payer_id").is(payerId).and("deleted").is(false)),
                group("provider_id").count().as("totalRequests").first("provider_name").as("providerName"),
                sort(Sort.Direction.DESC, "totalRequests"),
                limit(10)
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getPayerMonthlyTrend(String payerId) {
        Aggregation agg = newAggregation(
                match(Criteria.where("payer_id").is(payerId).and("deleted").is(false)),
                project().andExpression("month(reviewed_at)").as("month").andExpression("year(reviewed_at)").as("year"),
                group(Fields.fields("year", "month")).count().as("count"),
                sort(Sort.Direction.ASC, "year", "month"),
                limit(12)
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    private List<Map<String, Object>> getPayerMonthlyBreakdown(String payerId) {
        Aggregation agg = newAggregation(
                match(Criteria.where("payer_id").is(payerId)
                        .and("deleted").is(false)
                        .and("status").in("APPROVED", "REJECTED", "MORE_INFO_REQUIRED")),
                project().andExpression("month(reviewed_at)").as("month")
                         .andExpression("year(reviewed_at)").as("year")
                         .and("status").as("status"),
                group(Fields.fields("year", "month", "status")).count().as("count"),
                sort(Sort.Direction.ASC, "_id.year", "_id.month")
        );
        return toMapList(mongoTemplate.aggregate(agg, "authorizations", Document.class).getMappedResults());
    }

    /**
     * Converts a list of BSON {@link Document} objects to {@code List<Map<String, Object>>}
     * for JSON serialization — eliminates raw-type unchecked cast warnings.
     */
    private List<Map<String, Object>> toMapList(List<Document> docs) {
        return docs.stream()
                .map(doc -> (Map<String, Object>) new HashMap<>(doc))
                .toList();
    }
}
