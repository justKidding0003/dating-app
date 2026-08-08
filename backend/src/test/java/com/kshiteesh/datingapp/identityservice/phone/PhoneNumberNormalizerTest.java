package com.kshiteesh.datingapp.identityservice.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumberNormalizerTest {

	private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

	@Test
	void normalizeToE164AcceptsValidInternationalNumberWithoutRegion() {
		String normalized = normalizer.normalizeToE164("+1 650-253-0000", null);

		assertThat(normalized).isEqualTo("+16502530000");
	}

	@Test
	void normalizeToE164AcceptsValidNationalNumberWithExplicitRegion() {
		String normalized = normalizer.normalizeToE164("020 7031 3000", "GB");

		assertThat(normalized).isEqualTo("+442070313000");
	}

	@Test
	void normalizeToE164RejectsInvalidPhoneNumber() {
		assertThatThrownBy(() -> normalizer.normalizeToE164("12345", "US"))
				.isInstanceOf(InvalidPhoneNumberException.class);
	}

	@Test
	void normalizeToE164RequiresRegionForNationalNumber() {
		assertThatThrownBy(() -> normalizer.normalizeToE164("020 7031 3000", null))
				.isInstanceOf(InvalidPhoneNumberException.class);
	}

	@Test
	void normalizeToE164ProducesSameValueForEquivalentRepresentations() {
		String international = normalizer.normalizeToE164("+44 20 7031 3000", null);
		String national = normalizer.normalizeToE164("020 7031 3000", "gb");

		assertThat(international).isEqualTo(national);
	}
}
