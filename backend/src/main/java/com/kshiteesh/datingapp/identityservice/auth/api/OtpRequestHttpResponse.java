package com.kshiteesh.datingapp.identityservice.auth.api;

import java.time.Instant;
import java.util.UUID;

public record OtpRequestHttpResponse(
		UUID challengeId,
		Instant expiresAt,
		Instant resendAvailableAt
) {
}
