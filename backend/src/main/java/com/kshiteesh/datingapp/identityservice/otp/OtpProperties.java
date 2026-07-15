package com.kshiteesh.datingapp.identityservice.otp;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.otp")
public record OtpProperties(
		int length,
		Duration ttl,
		Duration resendCooldown,
		int requestLimit,
		Duration requestLimitWindow,
		int maxVerificationAttempts,
		String deliveryProvider
) {
	public OtpProperties {
		if (length < 4 || length > 10) {
			throw new InvalidOtpConfigurationException("OTP length must be between 4 and 10 digits.");
		}
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			throw new InvalidOtpConfigurationException("OTP TTL must be positive.");
		}
		if (resendCooldown == null || resendCooldown.isZero() || resendCooldown.isNegative()) {
			throw new InvalidOtpConfigurationException("OTP resend cooldown must be positive.");
		}
		if (requestLimit <= 0) {
			throw new InvalidOtpConfigurationException("OTP request limit must be positive.");
		}
		if (requestLimitWindow == null || requestLimitWindow.isZero() || requestLimitWindow.isNegative()) {
			throw new InvalidOtpConfigurationException("OTP request-limit window must be positive.");
		}
		if (maxVerificationAttempts <= 0) {
			throw new InvalidOtpConfigurationException("OTP maximum verification attempts must be positive.");
		}
	}
}
