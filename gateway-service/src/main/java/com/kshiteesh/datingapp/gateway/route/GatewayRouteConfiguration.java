package com.kshiteesh.datingapp.gateway.route;

import com.kshiteesh.datingapp.gateway.config.GatewayRouteProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfiguration {

	@Bean
	RouteLocator gatewayRoutes(RouteLocatorBuilder builder, GatewayRouteProperties routeProperties) {
		return builder.routes()
				.route("identity-service", route -> route
						.path("/identity/**")
						.uri(routeProperties.identityServiceUri()))
				.build();
	}
}
