package com.kshiteesh.datingapp.identityservice.token.access;

import com.kshiteesh.datingapp.identityservice.security.SecurityProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {

	private static final JWSAlgorithm ACCESS_TOKEN_ALGORITHM = JWSAlgorithm.RS256;

	private final SecurityProperties securityProperties;
	private final Clock clock;
	private final RSASSASigner signer;
	private final JWSVerifier verifier;

	public AccessTokenService(SecurityProperties securityProperties, Clock clock) {
		this.securityProperties = securityProperties;
		this.clock = clock;
		RSAPrivateKey privateKey = PemKeyParser.parseRsaPrivateKey(securityProperties.jwtPrivateKeyPem());
		RSAPublicKey publicKey = PemKeyParser.parseRsaPublicKey(securityProperties.jwtPublicKeyPem());
		this.signer = new RSASSASigner(privateKey);
		this.verifier = new RSASSAVerifier(publicKey);
		validateConfiguration(securityProperties);
	}

	public AccessToken issue(UUID identityId) {
		if (identityId == null) {
			throw new IllegalArgumentException("identityId must not be null");
		}
		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plus(securityProperties.accessTokenTtl());
		UUID tokenId = UUID.randomUUID();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.issuer(securityProperties.jwtIssuer())
				.subject(identityId.toString())
				.audience(securityProperties.jwtAudience())
				.issueTime(Date.from(issuedAt))
				.expirationTime(Date.from(expiresAt))
				.jwtID(tokenId.toString())
				.build();
		JWSHeader header = new JWSHeader.Builder(ACCESS_TOKEN_ALGORITHM)
				.keyID(securityProperties.jwtKeyId())
				.type(com.nimbusds.jose.JOSEObjectType.JWT)
				.build();
		SignedJWT signedJwt = new SignedJWT(header, claims);
		try {
			signedJwt.sign(signer);
		} catch (JOSEException ex) {
			throw new AccessTokenException("Access token signing failed.", ex);
		}
		return new AccessToken(signedJwt.serialize(), tokenId, issuedAt, expiresAt);
	}

	public AccessTokenClaims verify(String token) {
		try {
			SignedJWT signedJwt = SignedJWT.parse(token);
			if (!ACCESS_TOKEN_ALGORITHM.equals(signedJwt.getHeader().getAlgorithm())) {
				throw new AccessTokenException("Access token algorithm is not supported.");
			}
			if (!signedJwt.verify(verifier)) {
				throw new AccessTokenException("Access token signature is invalid.");
			}
			JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
			Instant now = Instant.now(clock);
			if (!securityProperties.jwtIssuer().equals(claims.getIssuer())) {
				throw new AccessTokenException("Access token issuer is invalid.");
			}
			List<String> audience = claims.getAudience();
			if (audience == null || !audience.contains(securityProperties.jwtAudience())) {
				throw new AccessTokenException("Access token audience is invalid.");
			}
			if (claims.getExpirationTime() == null || !now.isBefore(claims.getExpirationTime().toInstant())) {
				throw new AccessTokenException("Access token has expired.");
			}
			return new AccessTokenClaims(
					UUID.fromString(claims.getSubject()),
					UUID.fromString(claims.getJWTID()),
					claims.getIssuer(),
					securityProperties.jwtAudience(),
					claims.getIssueTime().toInstant(),
					claims.getExpirationTime().toInstant(),
					signedJwt.getHeader().getKeyID());
		} catch (ParseException | JOSEException | IllegalArgumentException ex) {
			if (ex instanceof AccessTokenException accessTokenException) {
				throw accessTokenException;
			}
			throw new AccessTokenException("Access token verification failed.", ex);
		}
	}

	private static void validateConfiguration(SecurityProperties securityProperties) {
		if (securityProperties.jwtKeyId() == null || securityProperties.jwtKeyId().isBlank()) {
			throw new InvalidTokenConfigurationException("JWT key id is required.");
		}
		if (securityProperties.jwtIssuer() == null || securityProperties.jwtIssuer().isBlank()) {
			throw new InvalidTokenConfigurationException("JWT issuer is required.");
		}
		if (securityProperties.jwtAudience() == null || securityProperties.jwtAudience().isBlank()) {
			throw new InvalidTokenConfigurationException("JWT audience is required.");
		}
		if (securityProperties.accessTokenTtl() == null || securityProperties.accessTokenTtl().isZero()
				|| securityProperties.accessTokenTtl().isNegative()) {
			throw new InvalidTokenConfigurationException("Access token TTL must be positive.");
		}
	}
}
