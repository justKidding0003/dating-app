package com.kshiteesh.datingapp.identityservice.token.access;

import java.time.Instant;
import java.util.UUID;

public record AccessToken(
		String value,
		UUID tokenId,
		Instant issuedAt,
		Instant expiresAt
) {
}
