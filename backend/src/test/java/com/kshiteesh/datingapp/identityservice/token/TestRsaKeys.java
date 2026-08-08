package com.kshiteesh.datingapp.identityservice.token;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class TestRsaKeys {

	private TestRsaKeys() {
	}

	public static TestKeyPair generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair keyPair = generator.generateKeyPair();
			return new TestKeyPair(
					toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()),
					toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("RSA is not available for tests.", ex);
		}
	}

	private static String toPem(String label, byte[] der) {
		return "-----BEGIN " + label + "-----\n"
				+ Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
				+ "\n-----END " + label + "-----";
	}

	public record TestKeyPair(String privateKeyPem, String publicKeyPem) {
	}
}
