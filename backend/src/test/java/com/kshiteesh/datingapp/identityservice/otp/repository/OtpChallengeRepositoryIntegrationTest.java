package com.kshiteesh.datingapp.identityservice.otp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallenge;
import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus;
import com.kshiteesh.datingapp.identityservice.token.access.AccessTokenService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OtpChallengeRepositoryIntegrationTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("identity_service_test")
			.withUsername("identity_test")
			.withPassword("identity_test");

	@Autowired
	private OtpChallengeRepository otpChallengeRepository;

	@MockitoBean
	private AccessTokenService accessTokenService;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRESQL::getUsername);
		registry.add("spring.datasource.password", POSTGRESQL::getPassword);
		registry.add("identity.otp.delivery-provider", () -> "local");
	}

	@Test
	void mapsOtpChallengeToApprovedFlywaySchema() {
		UUID challengeId = UUID.randomUUID();
		Instant now = Instant.parse("2026-07-09T00:00:00Z");
		OtpChallenge challenge = OtpChallenge.pending(
				challengeId,
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
				"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
				now,
				now.plusSeconds(300),
				now.plusSeconds(60),
				5);

		otpChallengeRepository.saveAndFlush(challenge);

		OtpChallenge persisted = otpChallengeRepository.findById(challengeId).orElseThrow();
		assertThat(persisted.getChallengeId()).isEqualTo(challengeId);
		assertThat(persisted.getIdentityId()).isNull();
		assertThat(persisted.getStatus()).isEqualTo(OtpChallengeStatus.PENDING);
		assertThat(persisted.getAttemptCount()).isZero();
		assertThat(persisted.getMaxAttempts()).isEqualTo(5);
	}

	@Test
	void expiresOlderPendingChallengesForSamePhoneLookupHash() {
		Instant now = Instant.parse("2026-07-09T00:00:00Z");
		String phoneLookupHash = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
		OtpChallenge olderChallenge = OtpChallenge.pending(
				UUID.randomUUID(),
				phoneLookupHash,
				"1111111111111111111111111111111111111111111111111111111111111111",
				now,
				now.plusSeconds(300),
				now.plusSeconds(60),
				5);
		OtpChallenge newerChallenge = OtpChallenge.pending(
				UUID.randomUUID(),
				phoneLookupHash,
				"2222222222222222222222222222222222222222222222222222222222222222",
				now.plusSeconds(61),
				now.plusSeconds(361),
				now.plusSeconds(121),
				5);
		otpChallengeRepository.saveAllAndFlush(List.of(olderChallenge, newerChallenge));

		int expiredCount = otpChallengeRepository.expireOtherPendingChallenges(
				phoneLookupHash,
				newerChallenge.getChallengeId());

		assertThat(expiredCount).isEqualTo(1);
		assertThat(otpChallengeRepository.findById(olderChallenge.getChallengeId()).orElseThrow().getStatus())
				.isEqualTo(OtpChallengeStatus.EXPIRED);
		assertThat(otpChallengeRepository.findById(newerChallenge.getChallengeId()).orElseThrow().getStatus())
				.isEqualTo(OtpChallengeStatus.PENDING);
	}

	@Test
	@Transactional
	void canLoadChallengeWithPessimisticLockForSingleConsumptionBoundary() {
		UUID challengeId = UUID.randomUUID();
		Instant now = Instant.parse("2026-07-09T00:00:00Z");
		otpChallengeRepository.saveAndFlush(OtpChallenge.pending(
				challengeId,
				"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
				"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
				now,
				now.plusSeconds(300),
				now.plusSeconds(60),
				5));

		assertThat(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).isPresent();
	}
}
