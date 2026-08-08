package com.kshiteesh.datingapp.gateway.security;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class RsaPublicKeyParser {
	private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
	private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

	private RsaPublicKeyParser() {
	}

	static RSAPublicKey parse(String configuredPem) {
		try {
			String encodedKey = configuredPem.replace("\\n", "\n")
					.replace(BEGIN_PUBLIC_KEY, "")
					.replace(END_PUBLIC_KEY, "")
					.replaceAll("\\s", "");
			byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
			return (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(keyBytes));
		}
		catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
			throw new IllegalStateException(
					"gateway.security.jwt.public-key-pem is not a valid RSA public key", exception);
		}
	}
}
