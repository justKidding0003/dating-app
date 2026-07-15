package com.kshiteesh.datingapp.identityservice.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.security")
public record SecurityProperties(
		String jwtKeyId,
		String jwtPrivateKeyPem,
		String jwtPublicKeyPem,
		String jwtIssuer,
		String jwtAudience,
		String otpHmacSecret,
		String phoneHmacSecret,
		String refreshTokenHmacSecret,
		Duration accessTokenTtl,
		Duration refreshTokenTtl
) {
}
