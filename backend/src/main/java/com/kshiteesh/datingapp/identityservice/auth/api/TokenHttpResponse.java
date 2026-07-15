package com.kshiteesh.datingapp.identityservice.auth.api;

import java.time.Instant;

public record TokenHttpResponse(
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt
) {

	@Override
	public String toString() {
		return "TokenHttpResponse[accessToken=<redacted>, accessTokenExpiresAt=" + accessTokenExpiresAt
				+ ", refreshToken=<redacted>, refreshTokenExpiresAt=" + refreshTokenExpiresAt + "]";
	}
}
