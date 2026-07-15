package com.kshiteesh.datingapp.identityservice.otp.service;

import com.kshiteesh.datingapp.identityservice.security.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OtpProtector {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String CANONICAL_SEPARATOR = ":otp:";

	private final SecretKeySpec secretKeySpec;

	@Autowired
	public OtpProtector(SecurityProperties securityProperties) {
		this(securityProperties.otpHmacSecret());
	}

	OtpProtector(String otpHmacSecret) {
		if (otpHmacSecret == null || otpHmacSecret.isBlank()) {
			throw new InvalidOtpProtectionConfigurationException("OTP protection secret is required.");
		}
		this.secretKeySpec = new SecretKeySpec(otpHmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}

	public String protect(UUID challengeId, String otp) {
		if (challengeId == null) {
			throw new IllegalArgumentException("challengeId must not be null");
		}
		if (otp == null || otp.isBlank()) {
			throw new IllegalArgumentException("otp must not be blank");
		}
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(secretKeySpec);
			byte[] digest = mac.doFinal(canonicalInput(challengeId, otp).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException("OTP protection is not available.", ex);
		}
	}

	public boolean matches(UUID challengeId, String submittedOtp, String expectedOtpHmac) {
		if (submittedOtp == null || submittedOtp.isBlank() || expectedOtpHmac == null || expectedOtpHmac.isBlank()) {
			return false;
		}
		return MessageDigest.isEqual(
				protect(challengeId, submittedOtp).getBytes(StandardCharsets.UTF_8),
				expectedOtpHmac.getBytes(StandardCharsets.UTF_8));
	}

	private static String canonicalInput(UUID challengeId, String otp) {
		return challengeId + CANONICAL_SEPARATOR + otp;
	}
}
