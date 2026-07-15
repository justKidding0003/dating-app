package com.kshiteesh.datingapp.identityservice.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * V1 single-instance rate limiter. State is instance-local, lost on restart, and
 * not shared across replicas; a shared store such as Redis is required before
 * horizontally scaled production deployment.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

	private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

	@Override
	public RateLimitDecision tryAcquire(String scope, String protectedKey, int limit, Duration window, Instant now) {
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be positive");
		}
		if (window == null || window.isZero() || window.isNegative()) {
			throw new IllegalArgumentException("window must be positive");
		}
		String rateLimitKey = scope + ":" + protectedKey;
		Deque<Instant> attempts = attemptsByKey.computeIfAbsent(rateLimitKey, ignored -> new ArrayDeque<>());
		synchronized (attempts) {
			Instant cutoff = now.minus(window);
			while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) {
				attempts.removeFirst();
			}
			if (attempts.size() >= limit) {
				return new RateLimitDecision(false, Duration.between(now, attempts.peekFirst().plus(window)));
			}
			attempts.addLast(now);
			return new RateLimitDecision(true, Duration.ZERO);
		}
	}
}
