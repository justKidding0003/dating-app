package com.kshiteesh.datingapp.identityservice.token.access;

import java.time.Instant;
import java.util.UUID;

public record AccessTokenClaims(
		UUID identityId,
		UUID tokenId,
		String issuer,
		String audience,
		Instant issuedAt,
		Instant expiresAt,
		String keyId
) {
}
