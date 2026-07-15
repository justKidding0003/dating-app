package com.kshiteesh.datingapp.identityservice.token.refresh;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
		String value,
		UUID sessionId,
		Instant expiresAt
) {
}
