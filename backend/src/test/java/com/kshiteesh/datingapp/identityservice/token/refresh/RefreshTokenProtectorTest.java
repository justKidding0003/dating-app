package com.kshiteesh.datingapp.identityservice.token.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenProtectorTest {

	@Test
	void protectsRefreshTokenDeterministicallyWithoutReturningRawToken() {
		RefreshTokenProtector protector = new RefreshTokenProtector("unit-test-refresh-token-secret");

		String first = protector.protect("raw-refresh-token");
		String second = protector.protect("raw-refresh-token");

		assertThat(first).isEqualTo(second);
		assertThat(first).isNotEqualTo("raw-refresh-token");
		assertThat(first).matches("[0-9a-f]{64}");
		assertThat(protector.matches("raw-refresh-token", first)).isTrue();
	}

	@Test
	void differentSecretsProduceDifferentHashes() {
		String first = new RefreshTokenProtector("first-secret").protect("raw-refresh-token");
		String second = new RefreshTokenProtector("second-secret").protect("raw-refresh-token");

		assertThat(first).isNotEqualTo(second);
	}
}
