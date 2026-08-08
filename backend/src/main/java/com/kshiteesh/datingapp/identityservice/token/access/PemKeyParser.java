package com.kshiteesh.datingapp.identityservice.token.access;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PemKeyParser {

	private PemKeyParser() {
	}

	static RSAPrivateKey parseRsaPrivateKey(String privateKeyPem) {
		try {
			byte[] der = decodePem(privateKeyPem, "PRIVATE KEY");
			PrivateKey privateKey = rsaKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(der));
			if (privateKey instanceof RSAPrivateKey rsaPrivateKey) {
				return rsaPrivateKey;
			}
			throw new InvalidTokenConfigurationException("JWT private key must be an RSA private key.");
		} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new InvalidTokenConfigurationException("JWT private key configuration is invalid.", ex);
		}
	}

	static RSAPublicKey parseRsaPublicKey(String publicKeyPem) {
		try {
			byte[] der = decodePem(publicKeyPem, "PUBLIC KEY");
			PublicKey publicKey = rsaKeyFactory().generatePublic(new X509EncodedKeySpec(der));
			if (publicKey instanceof RSAPublicKey rsaPublicKey) {
				return rsaPublicKey;
			}
			throw new InvalidTokenConfigurationException("JWT public key must be an RSA public key.");
		} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new InvalidTokenConfigurationException("JWT public key configuration is invalid.", ex);
		}
	}

	private static KeyFactory rsaKeyFactory() throws NoSuchAlgorithmException {
		return KeyFactory.getInstance("RSA");
	}

	private static byte[] decodePem(String pem, String label) {
		if (pem == null || pem.isBlank()) {
			throw new InvalidTokenConfigurationException("JWT " + label.toLowerCase() + " PEM is required.");
		}
		String normalized = pem
				.replace("-----BEGIN " + label + "-----", "")
				.replace("-----END " + label + "-----", "")
				.replace("\\n", "")
				.replaceAll("\\s", "");
		try {
			return Base64.getDecoder().decode(normalized);
		} catch (IllegalArgumentException ex) {
			throw new InvalidTokenConfigurationException("JWT " + label.toLowerCase() + " PEM is invalid.", ex);
		}
	}
}
