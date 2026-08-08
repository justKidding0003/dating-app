package com.kshiteesh.datingapp.identityservice.otp.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "identity.otp.delivery-provider", havingValue = "local")
public class LocalDevelopmentOtpDeliveryProvider implements OtpDeliveryProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalDevelopmentOtpDeliveryProvider.class);

	@Override
	public void deliver(String normalizedDestination, String otp) {
		LOGGER.warn("Local development OTP delivery: destination={} otp={}", mask(normalizedDestination), otp);
	}

	private static String mask(String normalizedDestination) {
		if (normalizedDestination == null || normalizedDestination.isBlank()) {
			return "unavailable";
		}
		String digits = normalizedDestination.replaceAll("\\D", "");
		if (digits.length() <= 4) {
			return "****";
		}
		return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
	}
}
