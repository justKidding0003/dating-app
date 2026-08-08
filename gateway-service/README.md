# Gateway Service

The Gateway Service is the single entry point for backend traffic in the privacy-first mutual connection platform. It fronts internal microservices and centralizes cross-cutting API concerns without changing service ownership boundaries.

## Architecture

The service is an independent Spring Boot 3 and Spring Cloud Gateway microservice.

- Runtime: Java 21
- Package root: `com.kshiteesh.datingapp.gateway`
- Gateway engine: Spring Cloud Gateway WebFlux
- Security: Spring Security WebFlux
- Health: Spring Boot Actuator liveness and readiness probes

In C4 terms, this service is the backend edge container. It receives external API traffic, applies gateway-level policies, and forwards approved requests to backend service containers. This Phase 1 implementation creates only the Identity Service route.

## Responsibilities

- Provide a stable backend entry point on port `8081`.
- Route `/identity/**` traffic to the Identity Service at `http://localhost:8080` by default.
- Validate Identity Service RS256 access tokens for every protected endpoint.
- Permit `/actuator/**` and `/identity/api/v1/auth/**` without authentication.
- Expose public health endpoints for deployment and operational checks.
- Log request id, method, path, response status, and execution time for each gateway request.
- Return a consistent JSON error response for gateway-level failures.

## Phase 2 Scope

Implemented in this phase:

- Independent Maven project named `gateway-service`
- Spring Cloud Gateway, Spring Security, Actuator, Validation, Configuration Processor, and Lombok dependencies
- Reactive security configuration with public health endpoint access
- Strongly typed issuer, audience, and RSA public-key configuration
- JWT signature, expiration, issuer, and audience validation
- Sanitized JSON 401 responses for missing or invalid bearer tokens
- Authentication event logging without token disclosure
- Identity Service route placeholder
- Request logging global filter
- Gateway exception handler
- Graceful shutdown and health probe configuration

## Future Phases

Phase 3 remains intentionally unimplemented. Its scope should be defined in the corresponding ADR before adding further gateway capabilities such as authorization policy, rate limiting, service discovery, resilience, or additional service routes.

## ADR References

Gateway design decisions should be captured in the project ADR set before Phase 2 hardens authentication and operational controls.
