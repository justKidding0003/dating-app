package com.kshiteesh.datingapp.identityservice.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequestHttpRequest(
		@NotBlank
		@Size(max = 32)
		String phoneNumber,

		@Size(max = 8)
		String regionCode
) {

	@Override
	public String toString() {
		return "OtpRequestHttpRequest[phoneNumber=<redacted>, regionCode=" + regionCode + "]";
	}
}
