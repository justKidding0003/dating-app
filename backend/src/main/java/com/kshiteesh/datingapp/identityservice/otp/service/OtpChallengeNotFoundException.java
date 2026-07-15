package com.kshiteesh.datingapp.identityservice.otp.service;

public class OtpChallengeNotFoundException extends RuntimeException {

	public OtpChallengeNotFoundException() {
		super("OTP challenge was not found.");
	}
}
