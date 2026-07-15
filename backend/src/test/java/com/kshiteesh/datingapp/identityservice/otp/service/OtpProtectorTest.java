package com.kshiteesh.datingapp.identityservice.otp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OtpProtectorTest {

	private static final UUID CHALLENGE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	private final OtpProtector protector = new OtpProtector("unit-test-otp-secret");

	@Test
	void sameChallengeOtpAndSecretProduceSameProtectedValue() {
		assertThat(protector.protect(CHALLENGE_ID, "012345"))
				.isEqualTo(protector.protect(CHALLENGE_ID, "012345"));
	}

	@Test
	void differentChallengeProducesDifferentProtectedValueForSameOtp() {
		String first = protector.protect(CHALLENGE_ID, "012345");
		String second = protector.protect(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"), "012345");

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void differentOtpProducesDifferentProtectedValueForSameChallenge() {
		String first = protector.protect(CHALLENGE_ID, "012345");
		String second = protector.protect(CHALLENGE_ID, "111111");

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void differentSecretProducesDifferentProtectedValue() {
		String first = new OtpProtector("first-secret").protect(CHALLENGE_ID, "012345");
		String second = new OtpProtector("second-secret").protect(CHALLENGE_ID, "012345");

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void matchesUsesProtectedComparisonPath() {
		String protectedOtp = protector.protect(CHALLENGE_ID, "012345");

		assertThat(protector.matches(CHALLENGE_ID, "012345", protectedOtp)).isTrue();
		assertThat(protector.matches(CHALLENGE_ID, "111111", protectedOtp)).isFalse();
	}

	@Test
	void protectedValueIsNotRawOtpAndFitsSchema() {
		String protectedOtp = protector.protect(CHALLENGE_ID, "012345");

		assertThat(protectedOtp).isNotEqualTo("012345");
		assertThat(protectedOtp).matches("[0-9a-f]{64}");
	}

	@Test
	void constructorRejectsBlankSecretWithoutExposingMaterial() {
		assertThatThrownBy(() -> new OtpProtector(" "))
				.isInstanceOf(InvalidOtpProtectionConfigurationException.class)
				.hasMessage("OTP protection secret is required.");
	}
}
