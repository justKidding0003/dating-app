package com.kshiteesh.datingapp.identityservice.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneLookupHasherTest {

	private final PhoneLookupHasher hasher = new PhoneLookupHasher("unit-test-phone-hmac-secret");

	@Test
	void hashProducesStableDeterministicLookupValue() {
		String firstHash = hasher.hash("+16502530000");
		String secondHash = hasher.hash("+16502530000");

		assertThat(firstHash).isEqualTo(secondHash);
		assertThat(firstHash).hasSize(64);
		assertThat(hasher.matches("+16502530000", firstHash)).isTrue();
	}

	@Test
	void hashProducesDifferentLookupValuesForDifferentPhoneNumbers() {
		String firstHash = hasher.hash("+16502530000");
		String secondHash = hasher.hash("+442070313000");

		assertThat(firstHash).isNotEqualTo(secondHash);
	}

	@Test
	void hashProducesDifferentLookupValuesForDifferentSecrets() {
		String firstHash = new PhoneLookupHasher("first-unit-test-secret").hash("+16502530000");
		String secondHash = new PhoneLookupHasher("second-unit-test-secret").hash("+16502530000");

		assertThat(firstHash).isNotEqualTo(secondHash);
	}

	@Test
	void hashUsesStableLowercaseHexEncoding() {
		String lookupHash = hasher.hash("+16502530000");

		assertThat(lookupHash).hasSize(64);
		assertThat(lookupHash).matches("[0-9a-f]{64}");
	}

	@Test
	void hashDoesNotReturnRawPhoneNumber() {
		String normalizedPhoneNumber = "+16502530000";

		assertThat(hasher.hash(normalizedPhoneNumber)).isNotEqualTo(normalizedPhoneNumber);
	}

	@Test
	void constructorRejectsBlankSecretWithoutExposingSecretMaterial() {
		assertThatThrownBy(() -> new PhoneLookupHasher(" "))
				.isInstanceOf(InvalidPhoneLookupConfigurationException.class)
				.hasMessage("Phone lookup protection secret is required.");
	}
}
