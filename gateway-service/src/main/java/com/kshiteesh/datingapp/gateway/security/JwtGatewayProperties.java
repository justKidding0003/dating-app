package com.kshiteesh.datingapp.gateway.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gateway.security.jwt")
public record JwtGatewayProperties(
		@NotBlank String issuer,
		@NotBlank String audience,
		@NotBlank String publicKeyPem
) {
}
