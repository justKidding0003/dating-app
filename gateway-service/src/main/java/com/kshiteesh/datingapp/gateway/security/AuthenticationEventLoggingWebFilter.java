package com.kshiteesh.datingapp.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class AuthenticationEventLoggingWebFilter implements WebFilter {
	private static final Logger log = LoggerFactory.getLogger(AuthenticationEventLoggingWebFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return ReactiveSecurityContextHolder.getContext()
				.map(context -> context.getAuthentication())
				.filter(Authentication::isAuthenticated)
				.doOnNext(authentication -> log.info(
						"authentication_success requestId={} path={} subject={}",
						RequestIdWebFilter.requestId(exchange),
						exchange.getRequest().getURI().getRawPath(),
						authentication.getName()))
				.then(chain.filter(exchange));
	}
}
