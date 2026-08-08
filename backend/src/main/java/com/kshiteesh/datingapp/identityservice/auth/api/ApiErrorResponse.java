package com.kshiteesh.datingapp.identityservice.auth.api;

import java.time.Instant;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message
) {
}
