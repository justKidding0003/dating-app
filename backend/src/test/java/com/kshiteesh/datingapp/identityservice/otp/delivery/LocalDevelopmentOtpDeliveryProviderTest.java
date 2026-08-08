package com.kshiteesh.datingapp.identityservice.otp.delivery;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LocalDevelopmentOtpDeliveryProviderTest {

	private final LocalDevelopmentOtpDeliveryProvider provider = new LocalDevelopmentOtpDeliveryProvider();

	@Test
	void deliverDoesNotFailForLocalDevelopment() {
		assertThatCode(() -> provider.deliver("+16502530000", "012345"))
				.doesNotThrowAnyException();
	}
}
