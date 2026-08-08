package com.kshiteesh.datingapp.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);
	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	private final ObjectMapper objectMapper;

	public GatewayExceptionHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
		if (exchange.getResponse().isCommitted()) {
			return Mono.error(exception);
		}

		HttpStatus status = resolveStatus(exception);
		String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
		ApiErrorResponse errorResponse = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.name(),
				status.getReasonPhrase());

		log.warn("gateway_error requestId={} path={} status={} message={}",
				requestId,
				exchange.getRequest().getURI().getRawPath(),
				status.value(),
				exception.getMessage());

		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		byte[] body = serialize(errorResponse);
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
	}

	private HttpStatus resolveStatus(Throwable exception) {
		if (exception instanceof ResponseStatusException responseStatusException) {
			return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	private byte[] serialize(ApiErrorResponse errorResponse) {
		try {
			return objectMapper.writeValueAsBytes(errorResponse);
		}
		catch (JsonProcessingException exception) {
			return "{\"status\":500,\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
		}
	}
}
