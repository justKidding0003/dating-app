package com.kshiteesh.datingapp.gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gateway.routes")
public record GatewayRouteProperties(
		@NotBlank String identityServiceUri
) {
}
