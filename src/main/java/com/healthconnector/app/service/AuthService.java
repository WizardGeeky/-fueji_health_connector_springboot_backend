package com.healthconnector.app.service;

import com.healthconnector.app.constants.*;
import com.healthconnector.app.dto.request.LoginRequest;
import com.healthconnector.app.dto.request.ChangePasswordRequest;
import com.healthconnector.app.dto.request.ForgotPasswordRequest;
import com.healthconnector.app.dto.request.ResetPasswordRequest;
import com.healthconnector.app.dto.response.AuthResponse;
import com.healthconnector.app.exception.*;
import com.healthconnector.app.model.User;
import com.healthconnector.app.repository.UserRepository;
import com.healthconnector.app.security.JwtTokenProvider;
import com.healthconnector.app.utils.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Authentication service handling login, password change, and token refresh.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.password-reset.expiry-minutes:60}")
    private int resetExpiryMinutes;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /**
     * Single login endpoint for all roles (SUPER_ADMIN, PROVIDER, PAYER).
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — unknown email: {}", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (user.isAccountLocked()) {
            log.warn("Login attempt on locked account: {}", user.getEmail());
            auditService.log(user.getId(), user.getEmail(), user.getRole().name(),
                    AuditAction.LOGIN_FAILED, "USER", user.getId(),
                    "Login attempt on locked account", httpRequest);
            throw new ForbiddenException("Account is locked. Please contact administrator.");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            log.warn("Login attempt on blocked account: {}", user.getEmail());
            throw new ForbiddenException("Account is blocked. Please contact your administrator.");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            log.warn("Login attempt on pending account: {}", user.getEmail());
            throw new ForbiddenException("Account is pending approval. Please contact your administrator to activate your account.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedAttempt(user, httpRequest);
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setFailedAttempts(0);
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getOrganizationId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        auditService.log(user.getId(), user.getEmail(), user.getRole().name(),
                AuditAction.LOGIN, "USER", user.getId(), "Successful login", httpRequest);

        notificationService.sendLoginAlertNotification(user, extractIp(httpRequest), extractDevice(httpRequest));

        log.info("Login successful for user={} role={}", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpirationMs)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .organizationId(user.getOrganizationId())
                .organizationName(user.getOrganizationName())
                .status(user.getStatus())
                .passwordChanged(user.isPasswordChanged())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getOrganizationId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpirationMs)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration
        userRepository.findByEmailAndDeletedFalse(request.getEmail()).ifPresent(user -> {
            String token  = UUID.randomUUID().toString();
            Instant expiry = Instant.now().plus(resetExpiryMinutes, ChronoUnit.MINUTES);
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(expiry);
            userRepository.save(user);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetLinkEmail(user.getEmail(), user.getFirstName(), resetLink);
            log.info("Password reset link sent | email={}", user.getEmail());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        passwordUtil.validatePasswordPolicy(request.getNewPassword());

        User user = userRepository.findByPasswordResetTokenAndDeletedFalse(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset link"));

        if (user.getPasswordResetTokenExpiry() == null
                || Instant.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new BusinessException("Reset link has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastAdminPassword(request.getNewPassword());
        user.setPasswordChanged(true);
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);
        log.info("Password reset successfully | email={}", user.getEmail());
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New password and confirm password do not match");
        }
        passwordUtil.validatePasswordPolicy(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastAdminPassword(request.getNewPassword());
        user.setPasswordChanged(true);
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        auditService.log(user.getId(), user.getEmail(), user.getRole().name(),
                AuditAction.PASSWORD_CHANGED, "USER", user.getId(), "Password changed", httpRequest);
        log.info("Password changed successfully for userId={}", userId);
    }

    private void handleFailedAttempt(User user, HttpServletRequest request) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setAccountLocked(true);
            userRepository.save(user);
            log.warn("Account locked for email={} after {} failed attempts", user.getEmail(), attempts);
            auditService.log(user.getId(), user.getEmail(), user.getRole().name(),
                    AuditAction.ACCOUNT_LOCKED, "USER", user.getId(),
                    "Account locked after " + attempts + " failed attempts", request);
        } else {
            userRepository.save(user);
            log.warn("Login failed for email={} attempts={}", user.getEmail(), attempts);
            auditService.log(user.getId(), user.getEmail(), user.getRole().name(),
                    AuditAction.LOGIN_FAILED, "USER", user.getId(),
                    "Failed login attempt " + attempts, request);
        }
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String fwd = request.getHeader("X-Forwarded-For");
        return fwd != null ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String extractDevice(HttpServletRequest request) {
        if (request == null) return "Unknown";
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("Mobile") || ua.contains("Android")) return "Mobile";
        return "Desktop";
    }
}
