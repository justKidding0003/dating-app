package com.kshiteesh.datingapp.identityservice.otp.service;

public class InvalidOtpException extends RuntimeException {

	public InvalidOtpException() {
		super("OTP verification failed.");
	}
}
