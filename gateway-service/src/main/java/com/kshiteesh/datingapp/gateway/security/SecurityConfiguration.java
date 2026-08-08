package com.kshiteesh.datingapp.gateway.security;

import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
	private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error("invalid_token", "The required audience is missing.", null);

	@Bean
	ReactiveJwtDecoder jwtDecoder(JwtGatewayProperties properties) {
		RSAPublicKey publicKey = RsaPublicKeyParser.parse(properties.publicKeyPem());
		NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
		OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(properties.audience())
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(properties.issuer()),
				audienceValidator));
		return decoder;
	}

	@Bean
	SecurityWebFilterChain securityWebFilterChain(
			ServerHttpSecurity http,
			ReactiveJwtDecoder jwtDecoder,
			JwtAuthenticationEntryPoint authenticationEntryPoint) {
		AuthenticationEventLoggingWebFilter authenticationLoggingFilter =
				new AuthenticationEventLoggingWebFilter();
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
				.logout(ServerHttpSecurity.LogoutSpec::disable)
				.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
				.authorizeExchange(authorize -> authorize
						.pathMatchers("/actuator/**", "/identity/api/v1/auth/**").permitAll()
						.anyExchange().authenticated())
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.jwtDecoder(jwtDecoder))
						.authenticationEntryPoint(authenticationEntryPoint))
				.addFilterAfter(authenticationLoggingFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.build();
	}
}
