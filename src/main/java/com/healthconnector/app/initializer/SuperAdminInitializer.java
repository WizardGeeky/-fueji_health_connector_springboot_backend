package com.healthconnector.app.initializer;

import com.healthconnector.app.constants.AppConstants;
import com.healthconnector.app.constants.UserRole;
import com.healthconnector.app.constants.UserStatus;
import com.healthconnector.app.model.User;
import com.healthconnector.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Super Admin initializer — idempotent on every startup.
 * Creates the default Super Admin only if none exists.
 */
@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${superadmin.email}")
    private String superAdminEmail;

    @Value("${superadmin.password}")
    private String superAdminPassword;

    @Value("${superadmin.first-name}")
    private String superAdminFirstName;

    @Value("${superadmin.last-name}")
    private String superAdminLastName;

    @Value("${superadmin.organization-name}")
    private String superAdminOrgName;

    @Override
    public void run(String... args) {
        log.info("==========================================================");
        log.info("  HealthConnector Platform — Startup Initialization");
        log.info("==========================================================");

        if (userRepository.existsByRoleAndDeletedFalse(UserRole.SUPER_ADMIN)) {
            log.info("Super Admin already exists. Skipping creation.");
        } else {
            log.info("Creating default Super Admin...");
            User superAdmin = User.builder()
                    .firstName(superAdminFirstName)
                    .lastName(superAdminLastName)
                    .email(superAdminEmail.toLowerCase().trim())
                    .mobile("+10000000000")
                    .password(passwordEncoder.encode(superAdminPassword))
                    .role(UserRole.SUPER_ADMIN)
                    .organizationId(UUID.randomUUID().toString())
                    .organizationName(superAdminOrgName)
                    .status(UserStatus.ACTIVE)
                    .passwordChanged(true)
                    .failedAttempts(0)
                    .accountLocked(false)
                    .deleted(false)
                    .createdBy(AppConstants.SYSTEM_USER)
                    .build();
            userRepository.save(superAdmin);

            log.info("Super Admin created | email={}", superAdminEmail);
            log.info("IMPORTANT: Change the password via POST /api/auth/change-password after first login.");
        }
        log.info("==========================================================");
        log.info("  Swagger UI  : http://localhost:8080/swagger-ui.html");
        log.info("  API Docs    : http://localhost:8080/api-docs");
        log.info("  Health      : http://localhost:8080/actuator/health");
        log.info("==========================================================");
    }
}
