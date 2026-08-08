package com.kshiteesh.datingapp.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kshiteesh.datingapp.gateway.exception.ApiErrorResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
	private static final String MESSAGE = "Invalid or expired access token.";
	private final ObjectMapper objectMapper;

	public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
		String requestId = RequestIdWebFilter.requestId(exchange);
		String path = exchange.getRequest().getURI().getRawPath();
		log.warn("authentication_failure requestId={} path={}", requestId, path);
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(), HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", MESSAGE);
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
		byte[] bytes = serialize(body);
		return exchange.getResponse().writeWith(
				Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
	}

	private byte[] serialize(ApiErrorResponse body) {
		try {
			return objectMapper.writeValueAsBytes(body);
		}
		catch (JsonProcessingException exception) {
			return "{}".getBytes(StandardCharsets.UTF_8);
		}
	}
}
