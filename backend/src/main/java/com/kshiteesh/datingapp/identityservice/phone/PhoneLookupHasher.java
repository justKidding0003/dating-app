package com.kshiteesh.datingapp.identityservice.phone;

import com.kshiteesh.datingapp.identityservice.security.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PhoneLookupHasher {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final SecretKeySpec secretKeySpec;

	@Autowired
	public PhoneLookupHasher(SecurityProperties securityProperties) {
		this(securityProperties.phoneHmacSecret());
	}

	PhoneLookupHasher(String phoneHmacSecret) {
		if (phoneHmacSecret == null || phoneHmacSecret.isBlank()) {
			throw new InvalidPhoneLookupConfigurationException("Phone lookup protection secret is required.");
		}
		this.secretKeySpec = new SecretKeySpec(phoneHmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}

	public String hash(String normalizedE164PhoneNumber) {
		if (normalizedE164PhoneNumber == null || normalizedE164PhoneNumber.isBlank()) {
			throw new IllegalArgumentException("normalizedE164PhoneNumber must not be blank");
		}
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(secretKeySpec);
			byte[] digest = mac.doFinal(normalizedE164PhoneNumber.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException("Phone lookup hashing is not available.", ex);
		}
	}

	public boolean matches(String normalizedE164PhoneNumber, String expectedLookupHash) {
		if (expectedLookupHash == null || expectedLookupHash.isBlank()) {
			return false;
		}
		return MessageDigest.isEqual(
				hash(normalizedE164PhoneNumber).getBytes(StandardCharsets.UTF_8),
				expectedLookupHash.getBytes(StandardCharsets.UTF_8));
	}
}
