package com.kshiteesh.datingapp.gateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtAuthenticationIntegrationTests {
	private static final String ISSUER = "identity-service";
	private static final String AUDIENCE = "dating-app";
	private static final KeyPair SIGNING_KEY = generateKeyPair();
	private static final KeyPair OTHER_KEY = generateKeyPair();

	@Autowired
	private WebTestClient webTestClient;

	@DynamicPropertySource
	static void jwtProperties(DynamicPropertyRegistry registry) {
		registry.add("gateway.security.jwt.public-key-pem", JwtAuthenticationIntegrationTests::publicKeyPem);
	}

	@Test
	void acceptsValidJwt() {
		getProtected(token(SIGNING_KEY, ISSUER, AUDIENCE, Instant.now().plusSeconds(300)))
				.expectStatus().isOk();
	}

	@Test
	void rejectsExpiredJwt() {
		assertUnauthorized(token(SIGNING_KEY, ISSUER, AUDIENCE, Instant.now().minusSeconds(300)));
	}

	@Test
	void rejectsInvalidSignature() {
		assertUnauthorized(token(OTHER_KEY, ISSUER, AUDIENCE, Instant.now().plusSeconds(300)));
	}

	@Test
	void rejectsInvalidIssuer() {
		assertUnauthorized(token(SIGNING_KEY, "another-issuer", AUDIENCE, Instant.now().plusSeconds(300)));
	}

	@Test
	void rejectsInvalidAudience() {
		assertUnauthorized(token(SIGNING_KEY, ISSUER, "another-audience", Instant.now().plusSeconds(300)));
	}

	@Test
	void rejectsMissingAuthorizationHeader() {
		webTestClient.get().uri("/test/protected").exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.code").isEqualTo("UNAUTHORIZED")
				.jsonPath("$.message").isEqualTo("Invalid or expired access token.");
	}

	private WebTestClient.ResponseSpec getProtected(String token) {
		return webTestClient.get()
				.uri("/test/protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.exchange();
	}

	private void assertUnauthorized(String token) {
		getProtected(token)
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.status").isEqualTo(401)
				.jsonPath("$.code").isEqualTo("UNAUTHORIZED")
				.jsonPath("$.message").isEqualTo("Invalid or expired access token.");
	}

	private static String token(KeyPair keyPair, String issuer, String audience, Instant expiresAt) {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject("user-123")
				.issuer(issuer)
				.audience(List.of(audience))
				.issueTime(Date.from(Instant.now()))
				.expirationTime(Date.from(expiresAt))
				.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
		try {
			jwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
			return jwt.serialize();
		}
		catch (JOSEException exception) {
			throw new IllegalStateException("Could not sign test JWT", exception);
		}
	}

	private static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA is unavailable", exception);
		}
	}

	private static String publicKeyPem() {
		String encoded = Base64.getMimeEncoder(64, new byte[] { '\n' })
				.encodeToString(((RSAPublicKey) SIGNING_KEY.getPublic()).getEncoded());
		return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
	}

	@TestConfiguration
	static class ProtectedTestRouteConfiguration {
		@Bean
		RouterFunction<ServerResponse> protectedTestRoute() {
			return RouterFunctions.route()
					.GET("/test/protected", request -> ServerResponse.ok().build())
					.build();
		}
	}
}
