package com.kshiteesh.datingapp.identityservice.otp.service;

public class OtpAttemptsExhaustedException extends RuntimeException {

	public OtpAttemptsExhaustedException() {
		super("OTP verification attempts are exhausted.");
	}
}
