package com.kshiteesh.datingapp.gateway.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {
	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String requestId = requestId(exchange);
		ServerHttpRequest request = exchange.getRequest().mutate()
				.headers(headers -> headers.set(REQUEST_ID_HEADER, requestId))
				.build();
		exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
		return chain.filter(exchange.mutate().request(request).build());
	}

	static String requestId(ServerWebExchange exchange) {
		return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER))
				.filter(value -> !value.isBlank())
				.orElseGet(() -> UUID.randomUUID().toString());
	}
}
