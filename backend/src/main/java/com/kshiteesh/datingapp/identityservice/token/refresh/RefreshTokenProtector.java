package com.kshiteesh.datingapp.identityservice.token.refresh;

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
public class RefreshTokenProtector {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final SecretKeySpec secretKeySpec;

	@Autowired
	public RefreshTokenProtector(SecurityProperties securityProperties) {
		this(securityProperties.refreshTokenHmacSecret());
	}

	RefreshTokenProtector(String refreshTokenHmacSecret) {
		if (refreshTokenHmacSecret == null || refreshTokenHmacSecret.isBlank()) {
			throw new RefreshTokenException("Refresh token protection secret is required.");
		}
		secretKeySpec = new SecretKeySpec(refreshTokenHmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}

	public String protect(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new IllegalArgumentException("refreshToken must not be blank");
		}
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(secretKeySpec);
			return HexFormat.of().formatHex(mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException("Refresh token protection is not available.", ex);
		}
	}

	public boolean matches(String refreshToken, String expectedHash) {
		if (refreshToken == null || refreshToken.isBlank() || expectedHash == null || expectedHash.isBlank()) {
			return false;
		}
		return MessageDigest.isEqual(
				protect(refreshToken).getBytes(StandardCharsets.UTF_8),
				expectedHash.getBytes(StandardCharsets.UTF_8));
	}
}
