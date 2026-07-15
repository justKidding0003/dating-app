package com.kshiteesh.datingapp.identityservice.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kshiteesh.datingapp.identityservice.security.SecurityProperties;
import com.kshiteesh.datingapp.identityservice.token.TestRsaKeys;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessTokenServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");

	@Test
	void signsRs256TokenThatCanBeVerifiedWithPublicKey() {
		TestRsaKeys.TestKeyPair keyPair = TestRsaKeys.generate();
		AccessTokenService service = new AccessTokenService(properties(keyPair, "identity-service", "dating-app"),
				Clock.fixed(NOW, ZoneOffset.UTC));
		UUID identityId = UUID.randomUUID();

		AccessToken token = service.issue(identityId);
		AccessTokenClaims claims = service.verify(token.value());

		assertThat(claims.identityId()).isEqualTo(identityId);
		assertThat(claims.issuer()).isEqualTo("identity-service");
		assertThat(claims.audience()).isEqualTo("dating-app");
		assertThat(claims.issuedAt()).isEqualTo(NOW);
		assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
	}

	@Test
	void tokenHeaderContainsRs256AndKid() throws ParseException {
		TestRsaKeys.TestKeyPair keyPair = TestRsaKeys.generate();
		AccessTokenService service = new AccessTokenService(properties(keyPair, "identity-service", "dating-app"),
				Clock.fixed(NOW, ZoneOffset.UTC));

		SignedJWT signedJWT = SignedJWT.parse(service.issue(UUID.randomUUID()).value());

		assertThat(signedJWT.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
		assertThat(signedJWT.getHeader().getKeyID()).isEqualTo("test-key");
	}

	@Test
	void tokenClaimsContainNoPhoneOrProfileData() throws ParseException {
		TestRsaKeys.TestKeyPair keyPair = TestRsaKeys.generate();
		AccessTokenService service = new AccessTokenService(properties(keyPair, "identity-service", "dating-app"),
				Clock.fixed(NOW, ZoneOffset.UTC));

		String claims = SignedJWT.parse(service.issue(UUID.randomUUID()).value()).getJWTClaimsSet().toJSONObject().toString();

		assertThat(claims).doesNotContain("phone", "profile", "private", "match", "chat", "reveal", "otp");
	}

	@Test
	void expiredTokenIsRejectedDeterministically() {
		TestRsaKeys.TestKeyPair keyPair = TestRsaKeys.generate();
		SecurityProperties properties = properties(keyPair, "identity-service", "dating-app");
		AccessTokenService issuer = new AccessTokenService(properties, Clock.fixed(NOW, ZoneOffset.UTC));
		AccessTokenService verifier = new AccessTokenService(properties, Clock.fixed(NOW.plus(Duration.ofMinutes(15)), ZoneOffset.UTC));
		String token = issuer.issue(UUID.randomUUID()).value();

		assertThatThrownBy(() -> verifier.verify(token))
				.isInstanceOf(AccessTokenException.class)
				.hasMessage("Access token has expired.");
	}

	@Test
	void invalidIssuerIsRejected() {
		TestRsaKeys.TestKeyPair keyPair = TestRsaKeys.generate();
		String token = new AccessTokenService(properties(keyPair, "identity-service", "dating-app"),
				Clock.fixed(NOW, ZoneOffset.UTC)).issue(UUID.randomUUID()).value();
		AccessTokenService verifier = new AccessTokenService(properties(keyPair, "other-issuer", "dating-app"),
				Clock.fixed(NOW, ZoneOffset.UTC));

		assertThatThrownBy(() -> verifier.verify(token))
				.isInstanceOf(AccessTokenException.class)
				.hasMessage("Access token issuer is invalid.");
	}

	@Test
	void invalidAudienceIsRejected() {
		TestRsaKeys.TestKeyPair keyPair = TestRsaKeys.generate();
		String token = new AccessTokenService(properties(keyPair, "identity-service", "dating-app"),
				Clock.fixed(NOW, ZoneOffset.UTC)).issue(UUID.randomUUID()).value();
		AccessTokenService verifier = new AccessTokenService(properties(keyPair, "identity-service", "other-audience"),
				Clock.fixed(NOW, ZoneOffset.UTC));

		assertThatThrownBy(() -> verifier.verify(token))
				.isInstanceOf(AccessTokenException.class)
				.hasMessage("Access token audience is invalid.");
	}

	private static SecurityProperties properties(TestRsaKeys.TestKeyPair keyPair, String issuer, String audience) {
		return new SecurityProperties(
				"test-key",
				keyPair.privateKeyPem(),
				keyPair.publicKeyPem(),
				issuer,
				audience,
				"otp-secret",
				"phone-secret",
				"refresh-token-secret",
				Duration.ofMinutes(15),
				Duration.ofDays(30));
	}
}
