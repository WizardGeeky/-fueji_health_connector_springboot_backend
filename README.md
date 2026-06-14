# ⚙️ HealthConnector — Backend API Server

> Spring Boot 3 REST API for the HealthConnector Prior Authorization Platform.  
> Handles authentication, authorization lifecycle management, AI clinical review, AES-256/GCM encryption, async notifications, and immutable audit logging.

---

## 📋 Table of Contents

1. [Overview](#-overview)
2. [Tech Stack](#-tech-stack)
3. [Project Structure](#-project-structure)
4. [Architecture & Layers](#-architecture--layers)
5. [Security & Authentication](#-security--authentication)
6. [Authorization Request Lifecycle](#-authorization-request-lifecycle)
7. [Data Encryption](#-data-encryption)
8. [AI Integration (Gemini)](#-ai-integration-gemini)
9. [Async Processing](#-async-processing)
10. [Exception Handling](#-exception-handling)
11. [Database Design](#-database-design)
12. [REST API Reference](#-rest-api-reference)
13. [Configuration Reference](#-configuration-reference)
14. [Installation & Running](#-installation--running)
15. [Logging & Monitoring](#-logging--monitoring)
16. [Troubleshooting](#-troubleshooting)

---

## 🌐 Overview

The HealthConnector backend is a **stateless REST API** built with Spring Boot 3 and Java 21. It serves three user roles — **SUPER_ADMIN**, **PROVIDER**, and **PAYER** — each with distinct capabilities enforced via JWT-based authentication and Spring Security method-level `@PreAuthorize` annotations.

**Core responsibilities:**
- 🔐 Stateless JWT authentication with BCrypt password hashing
- 📄 Full prior authorization request lifecycle (DRAFT → APPROVED/REJECTED)
- 🤖 Google Gemini AI clinical review integration
- 🔒 AES-256/GCM encryption of all patient PII at rest
- 📜 Immutable audit trail for every platform action
- 🔔 Async email notifications via Gmail SMTP
- 📊 Analytics endpoints for all three dashboards

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 21 (LTS) | Core language |
| **Spring Boot** | 3.4.3 | Application framework |
| **Spring Security** | 6.x (via Boot) | Authentication & method-level RBAC |
| **Spring Data MongoDB** | 4.x (via Boot) | MongoDB ORM & auditing |
| **Spring AI — Vertex AI Gemini** | 1.0.0-M6 | AI clinical review |
| **Spring Boot Actuator** | 3.4.3 | Health & metrics endpoints |
| **Spring Boot Mail** | 3.4.3 | Email notifications |
| **Spring Boot Cache** | 3.4.3 | Caffeine-backed caching |
| **Spring Boot WebFlux** | 3.4.3 | Reactive pipeline support |
| **JJWT** | 0.12.6 | JWT creation, signing & validation |
| **MapStruct** | 1.6.3 | DTO ↔ entity mapping |
| **Resilience4j** | 2.2.0 | Rate limiting & circuit breaker |
| **Caffeine Cache** | 3.1.8 | In-memory cache implementation |
| **Micrometer Prometheus** | Latest | Metrics export for Prometheus |
| **SpringDoc OpenAPI** | 2.8.8 | Swagger UI & OpenAPI 3 docs |
| **Logstash Logback Encoder** | 8.0 | Structured JSON log output |
| **Apache Commons Lang3** | 3.17.0 | Utility helpers |
| **Commons Codec** | 1.17.1 | Base64/hex encoding utilities |
| **Lombok** | Latest | Annotation-based boilerplate reduction |
| **MongoDB Atlas** | Cloud (Atlas) | Primary NoSQL database |
| **Gradle** | 8.x (wrapper included) | Build tool |
| **BCrypt** | Strength 12 | Password hashing |
| **AES-256/GCM** | Java JCE | Patient field encryption |

---

## 📁 Project Structure

```
healthconnector/
├── build.gradle.kts                          # Gradle build + compiler flags
└── src/
    ├── main/
    │   ├── java/com/healthconnector/app/
    │   │   │
    │   │   ├── HealthconnectorApplication.java      # @SpringBootApplication entry point
    │   │   │
    │   │   ├── 📂 config/                           # Spring Bean Configuration
    │   │   │   ├── AsyncConfig.java                 # @EnableAsync, ThreadPoolTaskExecutor
    │   │   │   ├── MongoConfig.java                 # @EnableMongoAuditing, AuditorAware
    │   │   │   └── SecurityConfig.java              # JWT filter chain, CORS, CSP headers
    │   │   │
    │   │   ├── 📂 constants/                        # Enums & static constants
    │   │   │   ├── AppConstants.java                # Collection names, field names
    │   │   │   ├── AuthorizationStatus.java         # DRAFT,SUBMITTED,UNDER_REVIEW...
    │   │   │   ├── UserRole.java                    # SUPER_ADMIN, PROVIDER, PAYER
    │   │   │   ├── AuditAction.java                 # LOGIN, AUTHORIZATION_APPROVED...
    │   │   │   ├── ErrorCodes.java                  # String constants for error codes
    │   │   │   └── NotificationType.java            # Email notification type enum
    │   │   │
    │   │   ├── 📂 controller/                       # REST Controllers
    │   │   │   ├── AuthController.java              # /api/auth/**
    │   │   │   ├── AuthorizationController.java     # /api/authorizations/**
    │   │   │   ├── UserController.java              # /api/users/**
    │   │   │   ├── ProviderController.java          # /api/providers/**
    │   │   │   ├── PayerController.java             # /api/payers/**
    │   │   │   ├── AnalyticsController.java         # /api/analytics/**
    │   │   │   ├── AuditController.java             # /api/audit-logs/**
    │   │   │   ├── ChatController.java              # /api/chat/**
    │   │   │   └── NotificationController.java      # /api/notifications/**
    │   │   │
    │   │   ├── 📂 dto/                              # Data Transfer Objects
    │   │   │   ├── request/
    │   │   │   │   ├── CreateAuthorizationRequest.java
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   ├── RegisterRequest.java
    │   │   │   │   └── UpdateProfileRequest.java
    │   │   │   └── response/
    │   │   │       ├── AuthorizationResponse.java
    │   │   │       ├── AuthResponse.java
    │   │   │       ├── AIReviewResponse.java
    │   │   │       ├── ApiResponse.java            # Generic success wrapper
    │   │   │       └── ApiErrorResponse.java       # Error envelope
    │   │   │
    │   │   ├── 📂 exception/                        # Custom Exceptions & Handler
    │   │   │   ├── GlobalExceptionHandler.java      # @RestControllerAdvice — all errors
    │   │   │   ├── ApiErrorResponse.java            # Error JSON structure
    │   │   │   ├── BusinessException.java           # 422 domain rule violations
    │   │   │   ├── ForbiddenException.java          # 403 access denied
    │   │   │   ├── UnauthorizedException.java       # 401 unauthenticated
    │   │   │   ├── ResourceNotFoundException.java   # 404 entity not found
    │   │   │   ├── DuplicateResourceException.java  # 409 duplicate entry
    │   │   │   ├── ValidationException.java         # 400 manual validation
    │   │   │   ├── AESException.java                # 500 encryption failure
    │   │   │   ├── GeminiIntegrationException.java  # 503 AI service error
    │   │   │   └── FHIRValidationException.java     # 400 FHIR validation error
    │   │   │
    │   │   ├── 📂 initializer/                      # Application startup hooks
    │   │   │   └── SuperAdminInitializer.java       # Creates default SUPER_ADMIN user
    │   │   │
    │   │   ├── 📂 model/                            # MongoDB @Document models
    │   │   │   ├── User.java                        # users collection
    │   │   │   ├── AuthorizationRequest.java        # authorizations collection
    │   │   │   ├── AuditLog.java                    # audit_logs collection
    │   │   │   ├── Notification.java                # notifications collection
    │   │   │   └── ChatThread.java                  # chat_threads collection
    │   │   │
    │   │   ├── 📂 repository/                       # Spring Data MongoDB Repos
    │   │   │   ├── UserRepository.java
    │   │   │   ├── AuthorizationRequestRepository.java
    │   │   │   ├── AuditLogRepository.java
    │   │   │   └── NotificationRepository.java
    │   │   │
    │   │   ├── 📂 security/                         # JWT & UserDetails
    │   │   │   ├── JwtAuthenticationFilter.java     # OncePerRequestFilter — token validation
    │   │   │   ├── JwtTokenProvider.java            # JWT sign, parse, validate
    │   │   │   ├── CustomUserDetailsService.java    # Loads UserDetails by userId
    │   │   │   └── SecurityConstants.java           # PUBLIC_URLS whitelist array
    │   │   │
    │   │   ├── 📂 service/                          # Business Logic Layer
    │   │   │   ├── AuthService.java                 # Login, register, password reset
    │   │   │   ├── AuthorizationRequestService.java # Full authorization lifecycle
    │   │   │   ├── GeminiAIService.java             # Vertex AI Gemini integration
    │   │   │   ├── NotificationService.java         # @Async email notifications
    │   │   │   ├── AuditService.java                # @Async audit log writes
    │   │   │   ├── FHIRService.java                 # FHIR R4 validation
    │   │   │   ├── AnalyticsService.java            # Dashboard aggregations
    │   │   │   └── UserService.java                 # User CRUD + admin operations
    │   │   │
    │   │   └── 📂 utils/                            # Utility Classes
    │   │       └── AESUtil.java                     # AES-256/GCM encrypt & decrypt
    │   │
    │   └── resources/
    │       └── application.properties               # All runtime configuration
    │
    └── test/                                        # Unit & integration tests
        └── java/com/healthconnector/app/
```

---

## 🏗️ Architecture & Layers

```
HTTP Request
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│                    Security Filter Chain                     │
│                                                              │
│  CorsFilter → HeaderWriterFilter → JwtAuthenticationFilter  │
│  → AuthorizationFilter → ExceptionTranslationFilter         │
└────────────────────────────┬─────────────────────────────────┘
                             │ (Authenticated Principal in SecurityContext)
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                  Controller Layer (@RestController)          │
│                                                              │
│  - Validates @RequestParam / @PathVariable                   │
│  - Delegates to Service (no business logic here)             │
│  - Returns ResponseEntity<ApiResponse<T>>                    │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                    Service Layer (@Service)                   │
│                                                              │
│  - @Transactional on write operations                        │
│  - @PreAuthorize RBAC checks                                 │
│  - AES encrypt/decrypt via AESUtil                           │
│  - Calls @Async AuditService + NotificationService           │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────┐
│               Repository Layer (Spring Data MongoDB)         │
│                                                              │
│  - Derived query methods (findByIdAndDeletedFalse, etc.)     │
│  - @Version optimistic locking on all documents             │
│  - @CreatedDate / @LastModifiedDate auditing                 │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
                     MongoDB Atlas (Cloud)
```

---

## 🔐 Security & Authentication

### JWT Authentication Flow

```
POST /api/auth/login  { email, password }
         │
         ▼
AuthService.authenticate()
  ├── Load user by email (throws 404 if not found)
  ├── Check status: ACTIVE required
  ├── Check account_locked: false required
  ├── BCrypt.verify(inputPassword, storedHash)
  │     ├── FAIL: increment failed_attempts
  │     │          if attempts >= 5 → lock account (30 min)
  │     │          throw 401 INVALID_CREDENTIALS
  │     └── OK:   reset failed_attempts, update last_login
  ├── Build JWT claims:
  │     sub=userId, email, role, organizationId, sessionId
  └── Sign with HS384, TTL=24h
         │
         ▼
Response: { token, refreshToken, user: {...} }
```

### JWT Token Claims

```json
{
  "sub": "6a2d53a9b115080049831ae3",
  "userId": "6a2d53a9b115080049831ae3",
  "email": "provider@hospital.com",
  "role": "PROVIDER",
  "organizationId": "1048dc86-262d-4f04-9eb2-2be495d2c5e8",
  "sessionId": "6eb7d5ef-aefc-4b51-8d59-874632...",
  "iat": 1781355464,
  "exp": 1781441864
}
```

### JwtAuthenticationFilter Pipeline

```java
// Runs on every request (OncePerRequestFilter)
1. Extract "Authorization: Bearer <token>" header
2. No token?  → pass through (public endpoints handled by SecurityConfig)
3. Parse JWT  → extract userId
4. Load UserDetails from MongoDB by userId
5. Set UsernamePasswordAuthenticationToken in SecurityContextHolder
6. ExpiredJwtException   → 401 { errorCode: "TOKEN_EXPIRED" }
7. MalformedJwtException → 401 { errorCode: "TOKEN_INVALID" }
```

### Role-Based Access Control

```
Endpoint access enforced by @PreAuthorize at method level:

@PreAuthorize("hasRole('PAYER')")
public AuthorizationResponse approveAuthorization(...) { ... }

@PreAuthorize("hasRole('PROVIDER')")
public AuthorizationResponse createDraft(...) { ... }

@PreAuthorize("hasRole('ADMIN')")
public Page<AuditLogResponse> getAllLogs(...) { ... }
```

### Security Headers (applied to every response)

| Header | Value |
|---|---|
| `Content-Security-Policy` | `default-src 'self'; frame-ancestors 'none'; script-src 'self'` |
| `X-Frame-Options` | `DENY` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `X-Content-Type-Options` | `nosniff` |
| Session Policy | `STATELESS` — no cookies, no sessions |
| CSRF | Disabled (stateless JWT — no session to protect) |

### Password Policies

| Policy | Value |
|---|---|
| Hash algorithm | BCrypt, cost factor 12 |
| Max failed attempts | 5 (then lock) |
| Lock duration | 30 minutes |
| Password expiry | 90 days |
| Min length | Validated at registration |

### Public (no auth required) URLs

```
/api/auth/login
/api/auth/register
/api/auth/refresh
/api/auth/forgot-password
/api/auth/reset-password
/swagger-ui/**
/api-docs/**
/actuator/health
```

---

## 📋 Authorization Request Lifecycle

### Status State Machine

```
DRAFT
  │  POST /api/authorizations/{id}/submit         (PROVIDER)
  ▼
SUBMITTED
  │  POST /api/authorizations/{id}/start-review   (PAYER)
  ▼
UNDER_REVIEW
  ├──▶  APPROVED          POST /{id}/approve       (PAYER)
  ├──▶  REJECTED          POST /{id}/reject        (PAYER)
  └──▶  MORE_INFO_REQUIRED POST /{id}/request-info (PAYER)
              │
              │  POST /{id}/provide-info  (PROVIDER adds notes)
              ▼
          UNDER_REVIEW  (re-enters review)

APPROVED / REJECTED
  │  POST /{id}/reconsider  (PAYER)
  ▼
UNDER_REVIEW
```

### Business Rules Enforced in Service

| Rule | Where enforced |
|---|---|
| Only PAYER can approve | `@PreAuthorize("hasRole('PAYER')")` + `enforcePayerAccess()` |
| Approve only from SUBMITTED/UNDER_REVIEW/MORE_INFO | `if (status != ...)` check in `approveAuthorization()` |
| Reconsider only from APPROVED/REJECTED | `if (status != ...)` check in `reconsiderAuthorization()` |
| Delete only DRAFT | `if (status != DRAFT)` in `softDelete()` |
| Payer must own the request | `auth.getPayerId().equals(userId)` in `enforcePayerAccess()` |

---

## 🔒 Data Encryption

### AES-256/GCM Encryption

All patient personally identifiable information (PII) and clinical notes are encrypted **before** being written to MongoDB.

**Encrypted fields on `AuthorizationRequest`:**

| Field | Type | Notes |
|---|---|---|
| `patientName` | String | Full name |
| `patientAddress` | String | Home address |
| `patientMobile` | String | Phone number |
| `insuranceNumber` | String | Insurance policy number |
| `memberId` | String | Member/subscriber ID |
| `diagnosisDescription` | String | ICD-10 description text |
| `procedureDescription` | String | CPT procedure description |
| `clinicalNotes` | String | Doctor's clinical notes |

**Encryption implementation (`AESUtil.java`):**

```
encrypt(plaintext):
  1. Generate random 12-byte nonce (IV)
  2. Init AES/GCM/NoPadding cipher with 256-bit key
  3. Encrypt plaintext → ciphertext + 128-bit auth tag
  4. Prepend IV to ciphertext
  5. Base64-encode the combined bytes → stored in MongoDB

decrypt(base64Ciphertext):
  1. Base64-decode
  2. Extract first 12 bytes as IV
  3. Init cipher with same key + IV
  4. Decrypt → plaintext
  5. GCM auth tag verification (detects tampering)
```

**Key management:**
```properties
# application.properties
aes.algorithm=AES/GCM/NoPadding
aes.key-size=256
aes.secret-key=<base64-encoded 32-byte key>
# Generate: openssl rand -base64 32
```

**Graceful fallback (`safeDecrypt`):**

```java
private String safeDecrypt(String value) {
    if (value == null) return null;
    try {
        return aesUtil.decrypt(value);
    } catch (Exception e) {
        // If stored before encryption was added, return raw value
        log.warn("AES decrypt failed, returning raw value | error={}", e.getMessage());
        return value;
    }
}
```

---

## 🤖 AI Integration (Gemini)

### Google Vertex AI — Gemini 2.5 Flash

**Model config:**
```properties
spring.ai.vertex.ai.gemini.chat.options.model=gemini-2.5-flash-lite
spring.ai.vertex.ai.gemini.chat.options.temperature=0.2
spring.ai.vertex.ai.gemini.chat.options.max-output-tokens=4096
```

**Review flow:**

```
POST /api/authorizations/{id}/ai-review   [PAYER only]
         │
         ▼
GeminiAIService.reviewAuthorization(authId, userId)
  1. Load authorization from MongoDB
  2. Decrypt: patientName, diagnosisDescription,
              procedureDescription, clinicalNotes
  3. Build clinical assessment prompt
  4. Call Vertex AI Gemini API (synchronous)
  5. Parse JSON response:
     ├── ai_score:       0-100 (clinical necessity score)
     ├── ai_risk_level:  LOW | MEDIUM | HIGH
     ├── recommendation: APPROVE | DENY | REQUEST_MORE_INFO
     └── flags:          [ "missing lab results", ... ]
  6. Persist ai_score + ai_risk_level + latest_ai_review_id
  7. Return AIReviewResponse to controller
```

**GeminiIntegrationException → 503** if the AI service is unavailable.

---

## ⚡ Async Processing

Two services run on a background thread pool to keep API response times fast:

```
AsyncConfig.java sets up:
  corePoolSize:    4
  maxPoolSize:    16
  queueCapacity: 100
  threadPrefix:  "Async-"
```

### AuditService (`@Async`)

```java
@Async
public void log(String userId, String userEmail, String userRole,
                AuditAction action, ...) {
    try {
        // Write AuditLog document to MongoDB
        auditLogRepository.save(auditLog);
    } catch (Exception e) {
        // Never throws — audit failure must not affect business operations
        log.error("Failed to write audit log: {}", e.getMessage());
    }
}
```

### NotificationService (`@Async`)

```java
@Async
public void sendAuthorizationNotification(String recipientUserId,
        NotificationType type, String refNumber, ...) {
    // Load recipient email from MongoDB
    // Build email subject + body based on NotificationType
    // Send via JavaMailSender (Gmail SMTP)
    // Fire-and-forget — exceptions logged but not rethrown
}
```

**Notification triggers:**

| Action | Recipient | Template |
|---|---|---|
| Authorization SUBMITTED | PAYER | "New authorization request assigned" |
| Authorization APPROVED | PROVIDER | "Your request was approved" |
| Authorization REJECTED | PROVIDER | "Your request was rejected" |
| More Info Required | PROVIDER | "Payer needs additional information" |
| Under Review again | PROVIDER | "Your request is under reconsideration" |

---

## ⚠️ Exception Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) catches all exceptions and returns a consistent JSON envelope:

```json
{
  "status": 422,
  "errorCode": "BUSINESS_RULE_VIOLATION",
  "message": "Cannot approve from status: DRAFT",
  "path": "/api/authorizations/abc/approve",
  "errors": null
}
```

### Exception → HTTP Status Mapping

| Exception | Status | Error Code |
|---|---|---|
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` |
| `DuplicateResourceException` | 409 | `DUPLICATE_RESOURCE` |
| `BusinessException` | 422 | Per-rule custom code |
| `ValidationException` | 400 | `VALIDATION_ERROR` |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` |
| `ForbiddenException` | 403 | `FORBIDDEN` |
| `AESException` | 500 | `AES_ENCRYPTION_ERROR` |
| `GeminiIntegrationException` | 503 | `GEMINI_INTEGRATION_ERROR` |
| `FHIRValidationException` | 400 | `FHIR_VALIDATION_ERROR` |
| `BadCredentialsException` | 401 | `INVALID_CREDENTIALS` |
| `LockedException` | 403 | `ACCOUNT_LOCKED` |
| `DisabledException` | 403 | `ACCOUNT_BLOCKED` |
| `AccessDeniedException` | 403 | `FORBIDDEN` |
| `DuplicateKeyException` (Mongo) | 409 | `DUPLICATE_RESOURCE` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `Exception` (fallback) | 500 | `INTERNAL_SERVER_ERROR` |
| `Throwable` (fallback) | 500 | `INTERNAL_SERVER_ERROR` |

### `ApiErrorResponse` structure (`@JsonInclude(NON_NULL)`)

```java
@Builder
public class ApiErrorResponse {
    int status;
    String errorCode;
    String message;
    String path;
    List<Map<String, String>> errors;  // null if not a validation error
}
```

---

## 🗄️ Database Design

### MongoDB Collection: `users`

```
Field               Type        Index           Notes
─────────────────   ──────────  ──────────────  ────────────────────────────
_id                 ObjectId    Primary         MongoDB auto-generated
email               String      Unique index    Login identifier
password            String                      BCrypt hash (never plaintext)
role                String                      SUPER_ADMIN | PROVIDER | PAYER
status              String                      ACTIVE | INACTIVE | BLOCKED
organization_id     String                      Links to an organization
organization_name   String
failed_attempts     int                         Incremented on bad login
account_locked      boolean                     True if attempts >= 5
password_changed    boolean                     False until user sets own password
last_login          ISODate
created_at          ISODate     @CreatedDate    Auto-set by Spring Data
updated_at          ISODate     @LastModifiedDate
created_by          String      @CreatedBy      Set by AuditorAware
deleted             boolean                     Soft-delete flag
version             Long        @Version        Optimistic locking counter
```

### MongoDB Collection: `authorizations`

```
Field                   Encrypted   Index              Notes
────────────────────    ─────────   ─────────────────  ──────────────────────
_id                     No          Primary
reference_number        No          Unique             HA-{ts}-{counter}
provider_id             No          Compound(+status)
payer_id                No          Compound(+status)
organization_id         No          Compound(+created)
patient_name            ✅ AES-GCM
patient_dob             No
patient_address         ✅ AES-GCM
patient_mobile          ✅ AES-GCM
insurance_number        ✅ AES-GCM
member_id               ✅ AES-GCM
primary_diagnosis_code  No
diagnosis_description   ✅ AES-GCM
procedure_code          No
procedure_description   ✅ AES-GCM
clinical_notes          ✅ AES-GCM
status                  No
ai_score                No
ai_risk_level           No
version                 No          @Version           Optimistic locking
deleted                 No                             Soft-delete
```

### MongoDB Collection: `audit_logs`

```
Field           Type        Notes
──────────────  ──────────  ─────────────────────────────────
_id             ObjectId    Auto-generated
user_id         String      Who performed the action
user_email      String
user_role       String
action          Enum        AuditAction enum value
entity_type     String      "AUTHORIZATION" | "USER" | ...
entity_id       String      ID of the affected entity
description     String      Human-readable summary
ip_address      String      X-Forwarded-For aware extraction
device          String      Desktop | Mobile | Tablet
timestamp       ISODate
success         boolean
```

### Compound Indexes

```javascript
// authorizations collection
db.authorizations.createIndex({ "provider_id": 1, "status": 1 })   // provider queue
db.authorizations.createIndex({ "payer_id": 1, "status": 1 })       // payer queue
db.authorizations.createIndex({ "organization_id": 1, "created_at": -1 })  // analytics

// users collection
db.users.createIndex({ "email": 1 }, { unique: true })
```

---

## 📡 REST API Reference

### 🔑 Authentication — `/api/auth`

| Method | Path | Auth | Request Body | Response |
|--------|------|------|-------------|----------|
| POST | `/login` | None | `{ email, password }` | `{ token, refreshToken, user }` |
| POST | `/register` | None | `{ firstName, lastName, email, password, role }` | User created |
| POST | `/refresh` | None | `{ refreshToken }` | `{ token }` |
| POST | `/logout` | Bearer | — | 200 OK |
| POST | `/forgot-password` | None | `{ email }` | Reset email sent |
| POST | `/reset-password` | None | `{ token, newPassword }` | Password updated |

### 📄 Authorizations — `/api/authorizations`

| Method | Path | Role | Query Params | Description |
|--------|------|------|-------------|-------------|
| GET | `/` | PROVIDER | `page, size` | My requests (paged) |
| GET | `/payer-queue` | PAYER | `page, size` | Assigned requests (paged) |
| GET | `/all` | ADMIN | `page, size` | All requests (paged) |
| POST | `/` | PROVIDER | — | Create draft |
| POST | `/{id}/submit` | PROVIDER | — | Submit draft |
| POST | `/{id}/start-review` | PAYER | — | Begin reviewing |
| POST | `/{id}/approve` | PAYER | `notes` (optional) | Approve |
| POST | `/{id}/reject` | PAYER | `reason` (optional) | Reject |
| POST | `/{id}/request-info` | PAYER | `notes` (required) | Request more info |
| POST | `/{id}/provide-info` | PROVIDER | `additionalNotes` (required) | Provide info |
| POST | `/{id}/reconsider` | PAYER | — | Reopen for review |
| POST | `/{id}/ai-review` | PAYER | — | Trigger Gemini AI |
| DELETE | `/{id}` | PROVIDER | — | Soft-delete draft |

### 👤 Users — `/api/users`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/providers` | ADMIN | Providers list (paged) |
| GET | `/payers` | ADMIN | Payers list (paged) |
| POST | `/providers` | ADMIN | Create provider |
| POST | `/payers` | ADMIN | Create payer |
| PUT | `/{id}` | ADMIN | Update user details |
| PATCH | `/{id}/status` | ADMIN | Toggle active/inactive |
| POST | `/{id}/reset-password` | ADMIN | Force-reset password |

### 👤 Profile — `/api/profile`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| PUT | `/` | Auth | Update own name/contact |
| PUT | `/password` | Auth | Change own password |

### 📊 Analytics — `/api/analytics`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/admin/dashboard` | ADMIN | Platform-wide KPIs |
| GET | `/provider/dashboard` | PROVIDER | Provider metrics + monthly trend |
| GET | `/payer/dashboard` | PAYER | Payer metrics + approval rate |

### 📋 Audit Logs — `/api/audit-logs`

| Method | Path | Role | Params | Description |
|--------|------|------|--------|-------------|
| GET | `/` | ADMIN | `page, size` | All logs (paged) |
| GET | `/user/{userId}` | ADMIN | `page, size` | Logs for a user |
| GET | `/entity/{entityId}` | ADMIN | `page, size` | Logs for an entity |

### 🔔 Notifications — `/api/notifications`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/` | Auth | My notifications (paged) |
| PATCH | `/{id}/read` | Auth | Mark notification as read |

### API Response Envelopes

**Success:**
```json
{
  "success": true,
  "message": "Approved",
  "data": { "id": "...", "status": "APPROVED", "referenceNumber": "HA-12345-1" }
}
```

**Error:**
```json
{
  "status": 403,
  "errorCode": "FORBIDDEN",
  "message": "You do not have access to this authorization request",
  "path": "/api/authorizations/123/approve",
  "errors": null
}
```

**Validation error:**
```json
{
  "status": 400,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/authorizations",
  "errors": [
    { "field": "patientName", "message": "must not be blank" },
    { "field": "procedureCode", "message": "must not be null" }
  ]
}
```

---

## 🔧 Configuration Reference

### `application.properties` — Full Reference

```properties
# ── Application ───────────────────────────────────────────
spring.application.name=healthconnector
server.port=8080
server.servlet.context-path=/
server.shutdown=graceful

# ── MongoDB ───────────────────────────────────────────────
spring.data.mongodb.uri=mongodb+srv://USER:PASS@cluster.mongodb.net/health?retryWrites=true&w=majority

# ── JWT ───────────────────────────────────────────────────
jwt.secret=<min 64 chars>
jwt.issuer=HealthConnector
jwt.audience=HealthConnectorUsers
jwt.expiration-ms=86400000            # 24 hours
jwt.refresh-expiration-ms=604800000   # 7 days

# ── AES Encryption ────────────────────────────────────────
aes.algorithm=AES/GCM/NoPadding
aes.key-size=256
aes.secret-key=<base64-encoded 32 bytes>

# ── Security Policies ────────────────────────────────────
app.security.max-failed-attempts=5
app.security.account-lock-minutes=30
app.security.password-expiry-days=90

# ── CORS ─────────────────────────────────────────────────
app.cors.allowed-origins=http://localhost:3000,http://localhost:4200

# ── Email (Gmail SMTP) ────────────────────────────────────
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<gmail>
spring.mail.password=<app-password>    # NOT regular password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# ── Super Admin (auto-created on first run) ───────────────
superadmin.email=admin@yourorg.com
superadmin.password=Admin@123
superadmin.first-name=Admin
superadmin.last-name=User
superadmin.organization-name=YourOrg Inc.

# ── Cache (Caffeine) ─────────────────────────────────────
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=30m

# ── Error Response ────────────────────────────────────────
server.error.include-message=always       # MUST be 'always' for error detail
server.error.include-stacktrace=never
server.error.whitelabel.enabled=false

# ── Gemini AI ─────────────────────────────────────────────
GOOGLE_CLOUD_PROJECT=your-gcp-project-id
spring.ai.vertex.ai.gemini.chat.options.model=gemini-2.5-flash-lite
spring.ai.vertex.ai.gemini.chat.options.temperature=0.2
spring.ai.vertex.ai.gemini.chat.options.max-output-tokens=4096

# ── Logging ───────────────────────────────────────────────
logging.level.root=INFO
logging.level.com.healthconnector=DEBUG
logging.level.org.springframework.security=WARN
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n
logging.file.name=logs/healthconnector.log

# ── Swagger ───────────────────────────────────────────────
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

---

## ⚡ Installation & Running

### Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Java JDK | 21 | `java -version` |
| Gradle Wrapper | Included | `./gradlew --version` |
| MongoDB Atlas | Cloud account | [atlas.mongodb.com](https://cloud.mongodb.com) |

### Steps

```bash
# 1. Navigate to backend folder
cd healthconnector

# 2. Configure application.properties (see Configuration Reference above)

# 3. Run with Gradle wrapper (REQUIRED — ensures -parameters flag)
./gradlew bootRun
```

> ✅ API server starts at **http://localhost:8080**  
> ✅ Swagger UI at **http://localhost:8080/swagger-ui.html**

### Build Production JAR

```bash
./gradlew build -x test
java -jar build/libs/healthconnector-1.0.0-SNAPSHOT.jar
```

### Important: Always use `./gradlew bootRun`

```
build.gradle.kts includes:
  tasks.withType<JavaCompile> {
      options.compilerArgs.addAll(listOf(
          "-parameters",           ← CRITICAL for @RequestParam name resolution
          "-Amapstruct.defaultComponentModel=spring",
          "-Xlint:-deprecation"
      ))
  }

Running from an IDE without "Delegate to Gradle" skips this flag,
causing IllegalArgumentException on @RequestParam-annotated endpoints.
```

---

## 📈 Logging & Monitoring

### Log Levels

| Package | Level |
|---|---|
| `com.healthconnector` | DEBUG |
| `org.springframework.security` | WARN |
| `org.mongodb.driver` | WARN |
| Root | INFO |

### Log Output

- **Console:** Human-readable pattern with timestamp, thread, level
- **File:** `logs/healthconnector.log` (rolling)
- **Format:** Logstash Logback Encoder available for JSON output in production

### Actuator Endpoints

```
/actuator/health   → { "status": "UP" }
```

> Full metrics exposure (Prometheus, info, etc.) is commented out in  
> `application.properties` — uncomment to enable.

---

## 🐛 Troubleshooting

### App fails to start
- **MongoDB connection error** — check URI, Atlas Network Access whitelist
- **Port 8080 in use** — change `server.port` or kill existing process
- **Java version mismatch** — must be Java 21: `java -version`

### 500 on approve/reject endpoints
- Always use `./gradlew bootRun`, not bare IDE run
- Check logs for actual exception — `GlobalExceptionHandler` wraps everything
- If "parameter name not available" error: Gradle compile is not active

### Email not delivered
- Use Gmail **App Password** (not account password): [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
- `@Async` — email errors appear in background thread logs, not in request log

### AI review returns 503
- Vertex AI API must be enabled in your GCP project
- Remove the `spring.autoconfigure.exclude` line and configure GCP credentials
- Check `GOOGLE_CLOUD_PROJECT` env variable is set

### MongoDB optimistic lock error
- Two concurrent requests tried to update the same document
- `@Version` field prevents a stale overwrite — let the client retry
