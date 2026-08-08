package com.kshiteesh.datingapp.identityservice.otp.service;

public class OtpChallengeExpiredException extends RuntimeException {

	public OtpChallengeExpiredException() {
		super("OTP challenge has expired.");
	}
}
