package com.kshiteesh.datingapp.identityservice.otp.service;

import java.time.Instant;
import java.util.UUID;

public record OtpChallengeRequestResult(
		UUID challengeId,
		Instant expiresAt,
		Instant resendAvailableAt
) {
}
