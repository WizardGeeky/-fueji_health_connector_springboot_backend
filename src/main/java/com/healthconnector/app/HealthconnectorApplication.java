package com.healthconnector.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Smart Healthcare Connector Platform
 *
 * <p>Production-grade Prior Authorization Platform with:
 * <ul>
 *   <li>Java 21 + Spring Boot 3.5</li>
 *   <li>MongoDB with AES-256/GCM deterministic encryption for PHI/PII</li>
 *   <li>JWT RBAC — SUPER_ADMIN | PROVIDER | PAYER</li>
 *   <li>Gemini AI clinical review via Spring AI</li>
 *   <li>FHIR R4 compliance validation</li>
 *   <li>Email notifications, audit logs, analytics</li>
 * </ul>
 *
 * <p>On startup, {@link com.healthconnector.app.initializer.SuperAdminInitializer}
 * creates the default Super Admin if none exists.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableMongoRepositories
public class HealthconnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthconnectorApplication.class, args);
    }
}
