package com.kshiteesh.datingapp.identityservice.token.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenGeneratorTest {

	@Test
	void generatedRefreshTokenIsOpaqueUrlSafeValue() {
		String token = new RefreshTokenGenerator().generate();

		assertThat(token).isNotBlank();
		assertThat(token).matches("[A-Za-z0-9_-]+");
		assertThat(token).hasSizeGreaterThanOrEqualTo(40);
	}
}
