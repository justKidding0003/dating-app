package com.kshiteesh.datingapp.identityservice.auth.api;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenHttpRequest(
		@NotBlank
		String refreshToken
) {

	@Override
	public String toString() {
		return "RefreshTokenHttpRequest[refreshToken=<redacted>]";
	}
}
