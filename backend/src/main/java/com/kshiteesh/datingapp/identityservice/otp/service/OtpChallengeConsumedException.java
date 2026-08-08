package com.kshiteesh.datingapp.identityservice.otp.service;

public class OtpChallengeConsumedException extends RuntimeException {

	public OtpChallengeConsumedException() {
		super("OTP challenge is no longer usable.");
	}
}
