package com.kshiteesh.datingapp.gateway.health;

import com.kshiteesh.datingapp.gateway.config.GatewayRouteProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("gatewayReadiness")
public class GatewayReadinessHealthIndicator implements HealthIndicator {

	private final GatewayRouteProperties routeProperties;

	public GatewayReadinessHealthIndicator(GatewayRouteProperties routeProperties) {
		this.routeProperties = routeProperties;
	}

	@Override
	public Health health() {
		return Health.up()
				.withDetail("identityServiceRoute", routeProperties.identityServiceUri())
				.withDetail("phase", "phase-1")
				.build();
	}
}
