package com.kshiteesh.datingapp.identityservice.otp.service;

import java.time.Instant;

public class ResendCooldownActiveException extends RuntimeException {

	private final Instant resendAvailableAt;

	public ResendCooldownActiveException(Instant resendAvailableAt) {
		super("OTP resend cooldown is active.");
		this.resendAvailableAt = resendAvailableAt;
	}

	public Instant getResendAvailableAt() {
		return resendAvailableAt;
	}
}
