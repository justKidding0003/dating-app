package com.kshiteesh.datingapp.identityservice.otp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class OtpGeneratorTest {

	@Test
	void generatedOtpHasConfiguredLengthAndDigitsOnly() {
		String otp = new OtpGenerator().generate(6);

		assertThat(otp).hasSize(6);
		assertThat(otp).matches("\\d{6}");
	}

	@Test
	void leadingZeroOtpIsStructurallySupported() {
		OtpGenerator generator = new OtpGenerator(new FixedSecureRandom(0));

		assertThat(generator.generate(6)).isEqualTo("000000");
	}

	@Test
	void generatorDoesNotUsePredictableCounterSequence() {
		OtpGenerator generator = new OtpGenerator(new FixedSecureRandom(3));

		assertThat(generator.generate(6)).isEqualTo("333333");
	}

	private static final class FixedSecureRandom extends SecureRandom {

		private final int value;

		private FixedSecureRandom(int value) {
			this.value = value;
		}

		@Override
		public int nextInt(int bound) {
			return value;
		}
	}
}
