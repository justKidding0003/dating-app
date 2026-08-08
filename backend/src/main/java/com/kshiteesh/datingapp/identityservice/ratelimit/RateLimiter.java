package com.kshiteesh.datingapp.identityservice.ratelimit;

import java.time.Duration;
import java.time.Instant;

public interface RateLimiter {

	RateLimitDecision tryAcquire(String scope, String protectedKey, int limit, Duration window, Instant now);
}
