# Identity Service Microservice - Quick Notes

This document summarizes what has been implemented so far in the Identity Service microservice and shows the architecture with renderable Mermaid diagrams.

To view diagrams in VS Code:

1. Open this file.
2. Press `Ctrl+Shift+V` for Markdown Preview.
3. If Mermaid does not render, install a Mermaid Markdown preview extension.

## What The Service Achieved

- Built a Spring Boot 3 / Java 21 identity microservice.
- Implemented package-by-feature structure: `auth`, `otp`, `identity`, `token`, `phone`, `ratelimit`, `security`, `common`.
- Added phone-number OTP authentication.
- Normalized phone numbers to E.164 before use.
- Stored phone lookup values as HMAC hashes instead of raw phone numbers.
- Created OTP challenges with expiry, resend cooldown, max attempts, and rate limiting.
- Added local development OTP delivery through `LocalDevelopmentOtpDeliveryProvider`.
- Issued RS256 JWT access tokens.
- Added opaque refresh tokens with server-side refresh sessions.
- Added refresh-token rotation, logout, expiry, revocation, and reuse detection.
- Added JPA entities and repositories for identities, OTP challenges, and refresh sessions.
- Added Flyway migrations for database schema.
- Added local development environment support with `.env.example`, ignored `.env.local`, and `run-local.ps1`.

## C4 Context

```mermaid
flowchart LR
    Client["Mobile / API Client"]
    IdentityService["Identity Service<br/>Spring Boot 3 / Java 21"]
    DB[("PostgreSQL<br/>identity, otp_challenge, refresh_session")]
    LocalOtp["Local OTP Delivery<br/>local profile only"]

    Client -->|"POST /api/v1/auth/*"| IdentityService
    IdentityService -->|"JPA repositories<br/>Flyway migrations"| DB
    IdentityService -->|"OtpDeliveryProvider.deliver()"| LocalOtp
    IdentityService -->|"challenge metadata<br/>access token<br/>refresh token"| Client
```

## Component/Class Dependency Diagram

```mermaid
classDiagram
    class AuthController
    class AuthenticationService
    class OtpChallengeService
    class OtpChallengePersistenceService
    class OtpGenerator
    class OtpProtector
    class OtpDeliveryProvider {
        <<interface>>
        +deliver(normalizedDestination, otp)
    }
    class LocalDevelopmentOtpDeliveryProvider
    class IdentityResolutionService
    class IdentityRepository {
        <<interface>>
    }
    class TokenOrchestrationService
    class AccessTokenService
    class RefreshSessionService
    class PhoneNumberNormalizer
    class PhoneLookupHasher
    class RateLimiter {
        <<interface>>
    }
    class InMemoryRateLimiter

    AuthController --> AuthenticationService
    AuthenticationService --> OtpChallengeService
    AuthenticationService --> IdentityResolutionService
    AuthenticationService --> TokenOrchestrationService
    AuthenticationService --> RefreshSessionService

    OtpChallengeService --> PhoneNumberNormalizer
    OtpChallengeService --> PhoneLookupHasher
    OtpChallengeService --> OtpGenerator
    OtpChallengeService --> OtpProtector
    OtpChallengeService --> OtpChallengePersistenceService
    OtpChallengeService --> OtpDeliveryProvider
    OtpChallengeService --> RateLimiter

    LocalDevelopmentOtpDeliveryProvider ..|> OtpDeliveryProvider
    InMemoryRateLimiter ..|> RateLimiter
    IdentityResolutionService --> IdentityRepository
    TokenOrchestrationService --> AccessTokenService
    TokenOrchestrationService --> RefreshSessionService
```

## OTP Request And Verify Sequence

```mermaid
sequenceDiagram
    actor Client
    participant AuthController
    participant AuthenticationService
    participant OtpChallengeService
    participant PhoneNumberNormalizer
    participant PhoneLookupHasher
    participant OtpGenerator
    participant OtpProtector
    participant OtpChallengeRepository
    participant LocalDevelopmentOtpDeliveryProvider
    participant IdentityResolutionService
    participant TokenOrchestrationService

    Client->>AuthController: POST /api/v1/auth/otp/request
    AuthController->>AuthenticationService: requestOtp(phoneNumber, regionCode)
    AuthenticationService->>OtpChallengeService: requestChallenge(...)
    OtpChallengeService->>PhoneNumberNormalizer: normalizeToE164(...)
    PhoneNumberNormalizer-->>OtpChallengeService: normalized E.164 phone
    OtpChallengeService->>PhoneLookupHasher: hash(normalizedPhone)
    PhoneLookupHasher-->>OtpChallengeService: phoneLookupHash
    OtpChallengeService->>OtpGenerator: generate(length)
    OtpGenerator-->>OtpChallengeService: otp
    OtpChallengeService->>OtpProtector: protect(challengeId, otp)
    OtpProtector-->>OtpChallengeService: otpHmac
    OtpChallengeService->>OtpChallengeRepository: save pending challenge
    OtpChallengeService->>LocalDevelopmentOtpDeliveryProvider: deliver(normalizedPhone, otp)
    OtpChallengeService-->>AuthenticationService: challengeId, expiresAt, resendAvailableAt
    AuthenticationService-->>AuthController: challenge result
    AuthController-->>Client: 202 Accepted

    Client->>AuthController: POST /api/v1/auth/otp/verify
    AuthController->>AuthenticationService: verifyOtpAndIssueTokens(...)
    AuthenticationService->>OtpChallengeService: verifyChallenge(challengeId, otp)
    OtpChallengeService->>OtpChallengeRepository: findByChallengeIdForUpdate(...)
    OtpChallengeService->>OtpProtector: matches(challengeId, submittedOtp, otpHmac)
    OtpProtector-->>OtpChallengeService: valid / invalid
    OtpChallengeService->>OtpChallengeRepository: mark consumed
    OtpChallengeService-->>AuthenticationService: phoneLookupHash
    AuthenticationService->>IdentityResolutionService: getOrCreateAfterSuccessfulAuthentication(hash)
    IdentityResolutionService-->>AuthenticationService: Identity
    AuthenticationService->>TokenOrchestrationService: issueAfterSuccessfulAuthentication(identityId, device)
    TokenOrchestrationService-->>AuthenticationService: accessToken + refreshToken
    AuthenticationService-->>AuthController: token result
    AuthController-->>Client: token response
```

## Persistence / Entity Diagram

```mermaid
erDiagram
    IDENTITY {
        uuid identity_id PK
        string phone_number_hash UK
        string status
        instant created_at
        instant updated_at
    }

    OTP_CHALLENGE {
        uuid challenge_id PK
        uuid identity_id
        string phone_number_hash
        string otp_hmac
        string status
        instant expires_at
        instant resend_available_at
        int attempt_count
        int max_attempts
        instant created_at
        instant verified_at
        instant consumed_at
    }

    REFRESH_SESSION {
        uuid session_id PK
        uuid identity_id
        string refresh_token_hash
        string previous_token_hash
        string status
        instant expires_at
        instant revoked_at
        instant rotated_at
        instant reuse_detected_at
        string device_id
        string device_name
        instant created_at
        instant updated_at
    }

    IDENTITY ||--o{ REFRESH_SESSION : owns
    IDENTITY ||--o{ OTP_CHALLENGE : authenticates_with
```

## Request Flow

```mermaid
flowchart TD
    A["Client submits phone number"] --> B["AuthController.requestOtp"]
    B --> C["AuthenticationService.requestOtp"]
    C --> D["OtpChallengeService.requestChallenge"]
    D --> E["Normalize phone number"]
    E --> F["Hash phone number"]
    F --> G["Check resend cooldown"]
    G --> H["Check rate limit"]
    H --> I["Generate OTP"]
    I --> J["Protect OTP with HMAC"]
    J --> K["Persist pending challenge"]
    K --> L["Deliver OTP locally"]
    L --> M["Return challenge metadata"]
```

## Verify Flow

```mermaid
flowchart TD
    A["Client submits challengeId + OTP"] --> B["AuthController.verifyOtp"]
    B --> C["AuthenticationService.verifyOtpAndIssueTokens"]
    C --> D["OtpChallengeService.verifyChallenge"]
    D --> E["Lock challenge row"]
    E --> F{"Pending and not expired?"}
    F -- "No" --> X["Throw domain exception"]
    F -- "Yes" --> G{"OTP HMAC matches?"}
    G -- "No" --> H["Record failed attempt"]
    G -- "Yes" --> I["Mark challenge consumed"]
    I --> J["Resolve or create Identity"]
    J --> K["Issue JWT access token"]
    K --> L["Create refresh session"]
    L --> M["Return tokens"]
```

## Main Classes

| Class | Package | Responsibility |
|---|---|---|
| `Application` | root | Spring Boot entry point and configuration property scanning. |
| `AuthController` | `auth.api` | Exposes authentication REST endpoints. |
| `AuthenticationService` | `auth.service` | Coordinates OTP, identity resolution, token issuance, refresh, and logout. |
| `OtpChallengeService` | `otp.service` | Handles OTP request and verification business workflow. |
| `OtpChallengePersistenceService` | `otp.service` | Handles transactional OTP challenge persistence. |
| `OtpGenerator` | `otp.service` | Generates numeric OTP values. |
| `OtpProtector` | `otp.service` | HMAC-protects OTP values and verifies submitted OTPs. |
| `LocalDevelopmentOtpDeliveryProvider` | `otp.delivery` | Logs local OTPs when `identity.otp.delivery-provider=local`. |
| `IdentityResolutionService` | `identity.service` | Finds or creates an identity after successful authentication. |
| `PhoneNumberNormalizer` | `phone` | Validates and normalizes phone numbers to E.164. |
| `PhoneLookupHasher` | `phone` | HMAC-hashes normalized phone numbers for private lookup. |
| `TokenOrchestrationService` | `token` | Issues and rotates token pairs. |
| `AccessTokenService` | `token.access` | Issues and verifies RS256 JWT access tokens. |
| `RefreshSessionService` | `token.refresh` | Manages refresh-token sessions and rotation. |
| `SecurityConfiguration` | `security` | Configures stateless Spring Security behavior. |

## Important Methods

| Class | Important functions |
|---|---|
| `AuthController` | `requestOtp`, `verifyOtp`, `refresh`, `logout` |
| `AuthenticationService` | `requestOtp`, `verifyOtpAndIssueTokens`, `refreshTokens`, `logout` |
| `OtpChallengeService` | `requestChallenge`, `verifyChallenge`, `enforceRequestRateLimit`, `enforceResendCooldown` |
| `IdentityResolutionService` | `findByPhoneLookupHash`, `existsByPhoneLookupHash`, `getOrCreateAfterSuccessfulAuthentication` |
| `PhoneNumberNormalizer` | `normalizeToE164` |
| `PhoneLookupHasher` | `hash`, `matches` |
| `TokenOrchestrationService` | `issueAfterSuccessfulAuthentication`, `rotate` |
| `AccessTokenService` | `issue`, `verify` |
| `RefreshSessionService` | `createSession`, `rotate`, `logout` |
| `LocalDevelopmentOtpDeliveryProvider` | `deliver`, `mask` |

## Import / Dependency Notes

| Area | Main imports used | Why |
|---|---|---|
| Spring Boot | `org.springframework.boot.*`, `org.springframework.stereotype.*`, `org.springframework.transaction.annotation.Transactional` | Application startup, service/component registration, transactions. |
| REST API | `org.springframework.web.bind.annotation.*`, `org.springframework.http.*` | HTTP endpoints and response status handling. |
| Validation | `jakarta.validation.*` | Validates request DTOs before service execution. |
| Persistence | `jakarta.persistence.*`, `org.springframework.data.jpa.repository.*` | Entity mapping and repository access. |
| Phone parsing | `com.google.i18n.phonenumbers.*` | Normalizes raw phone numbers to E.164. |
| JWT | `com.nimbusds.jose.*`, `com.nimbusds.jwt.*` | Signs and verifies RS256 JWT access tokens. |
| Crypto | `javax.crypto.Mac`, `javax.crypto.spec.SecretKeySpec`, `java.security.*` | HMAC protection for OTPs, phone lookup, refresh tokens, and RSA key parsing. |
| Time | `java.time.Clock`, `java.time.Instant`, `java.time.Duration` | Testable expiry, cooldown, TTL, and audit timestamps. |

## Endpoint Quick Notes

| Endpoint | Method | Purpose |
|---|---|---|
| `POST /api/v1/auth/otp/request` | `AuthController.requestOtp` | Create OTP challenge and deliver OTP. |
| `POST /api/v1/auth/otp/verify` | `AuthController.verifyOtp` | Verify OTP and issue token pair. |
| `POST /api/v1/auth/token/refresh` | `AuthController.refresh` | Rotate refresh token and issue a new token pair. |
| `POST /api/v1/auth/logout` | `AuthController.logout` | Revoke refresh token session. |

## Local Development Notes

Run the backend:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\backend\run-local.ps1
```

Health check:

```text
http://localhost:8080/actuator/health
```

If port `8080` is already in use:

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

