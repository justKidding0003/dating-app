package com.kshiteesh.datingapp.identityservice.ratelimit;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, Duration retryAfter) {
}
