package com.kshiteesh.datingapp.identityservice.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

	private final InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();

	@Test
	void firstFiveRequestsWithinFifteenMinuteWindowAreAllowedAndSixthIsRejected() {
		Instant now = Instant.parse("2026-07-09T00:00:00Z");
		Duration window = Duration.ofMinutes(15);
		String protectedPhoneHash = "protected-phone-hash";

		for (int minute = 0; minute < 5; minute++) {
			assertThat(rateLimiter.tryAcquire("otp-request", protectedPhoneHash, 5, window, now.plus(Duration.ofMinutes(minute * 2)))
					.allowed()).isTrue();
		}
		assertThat(rateLimiter.tryAcquire("otp-request", protectedPhoneHash, 5, window, now.plus(Duration.ofMinutes(9)))
				.allowed()).isFalse();
	}

	@Test
	void requestBecomesPossibleWhenOldAttemptLeavesSlidingWindow() {
		Instant now = Instant.parse("2026-07-09T10:00:00Z");
		Duration window = Duration.ofMinutes(15);
		String protectedPhoneHash = "protected-phone-hash";

		for (int minute = 0; minute < 5; minute++) {
			rateLimiter.tryAcquire("otp-request", protectedPhoneHash, 5, window, now.plus(Duration.ofMinutes(minute * 2)));
		}

		assertThat(rateLimiter.tryAcquire("otp-request", protectedPhoneHash, 5, window, now.plus(Duration.ofMinutes(15)).plusMillis(1))
				.allowed()).isTrue();
	}

	@Test
	void doesNotShareLimitsAcrossProtectedKeys() {
		Instant now = Instant.parse("2026-07-09T00:00:00Z");

		assertThat(rateLimiter.tryAcquire("otp-request", "first-protected-hash", 1, Duration.ofMinutes(5), now).allowed())
				.isTrue();
		assertThat(rateLimiter.tryAcquire("otp-request", "second-protected-hash", 1, Duration.ofMinutes(5), now).allowed())
				.isTrue();
	}
}
