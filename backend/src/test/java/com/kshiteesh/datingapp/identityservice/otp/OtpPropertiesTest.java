package com.kshiteesh.datingapp.identityservice.otp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OtpPropertiesTest {

	@Test
	void rejectsNonPositiveRequestLimit() {
		assertThatThrownBy(() -> new OtpProperties(6, Duration.ofMinutes(5), Duration.ofSeconds(60), 0,
				Duration.ofMinutes(15), 5, "local"))
				.isInstanceOf(InvalidOtpConfigurationException.class)
				.hasMessage("OTP request limit must be positive.");
	}

	@Test
	void rejectsNonPositiveRequestLimitWindow() {
		assertThatThrownBy(() -> new OtpProperties(6, Duration.ofMinutes(5), Duration.ofSeconds(60), 5,
				Duration.ZERO, 5, "local"))
				.isInstanceOf(InvalidOtpConfigurationException.class)
				.hasMessage("OTP request-limit window must be positive.");
	}

	@Test
	void rejectsNonPositiveResendCooldown() {
		assertThatThrownBy(() -> new OtpProperties(6, Duration.ofMinutes(5), Duration.ZERO, 5,
				Duration.ofMinutes(15), 5, "local"))
				.isInstanceOf(InvalidOtpConfigurationException.class)
				.hasMessage("OTP resend cooldown must be positive.");
	}
}
