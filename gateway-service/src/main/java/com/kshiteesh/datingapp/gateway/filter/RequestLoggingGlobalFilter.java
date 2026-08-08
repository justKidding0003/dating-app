package com.kshiteesh.datingapp.gateway.filter;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);
	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		long startedAtNanos = System.nanoTime();
		String requestId = resolveRequestId(exchange.getRequest());
		ServerHttpRequest request = exchange.getRequest().mutate()
				.header(REQUEST_ID_HEADER, requestId)
				.build();

		return chain.filter(exchange.mutate().request(request).build())
				.doFinally(signalType -> {
					long elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000;
					Integer statusCode = Optional.ofNullable(exchange.getResponse().getStatusCode())
							.map(status -> status.value())
							.orElse(null);
					log.info("gateway_request requestId={} method={} path={} status={} durationMs={}",
							requestId,
							request.getMethod(),
							request.getURI().getRawPath(),
							statusCode,
							elapsedMillis);
				});
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	private String resolveRequestId(ServerHttpRequest request) {
		return Optional.ofNullable(request.getHeaders().getFirst(REQUEST_ID_HEADER))
				.filter(value -> !value.isBlank())
				.orElseGet(() -> UUID.randomUUID().toString());
	}
}
