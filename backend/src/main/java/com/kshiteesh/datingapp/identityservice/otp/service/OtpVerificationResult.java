package com.kshiteesh.datingapp.identityservice.otp.service;

import java.time.Instant;
import java.util.UUID;

public record OtpVerificationResult(
		UUID challengeId,
		String phoneLookupHash,
		Instant verifiedAt
) {
}
