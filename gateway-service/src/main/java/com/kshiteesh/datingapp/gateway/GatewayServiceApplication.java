package com.kshiteesh.datingapp.gateway;

import com.kshiteesh.datingapp.gateway.config.GatewayRouteProperties;
import com.kshiteesh.datingapp.gateway.security.JwtGatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({ GatewayRouteProperties.class, JwtGatewayProperties.class })
public class GatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}
}
