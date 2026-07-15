package com.kshiteesh.datingapp.identityservice.otp.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class OtpGenerator {

	private static final String DIGITS = "0123456789";

	private final SecureRandom secureRandom;

	public OtpGenerator() {
		this(new SecureRandom());
	}

	OtpGenerator(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}

	public String generate(int length) {
		if (length <= 0) {
			throw new IllegalArgumentException("length must be positive");
		}
		StringBuilder otp = new StringBuilder(length);
		for (int index = 0; index < length; index++) {
			otp.append(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
		}
		return otp.toString();
	}
}
