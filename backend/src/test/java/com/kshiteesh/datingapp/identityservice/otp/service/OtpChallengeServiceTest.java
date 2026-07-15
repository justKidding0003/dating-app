package com.kshiteesh.datingapp.identityservice.otp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kshiteesh.datingapp.identityservice.identity.service.IdentityResolutionService;
import com.kshiteesh.datingapp.identityservice.otp.OtpProperties;
import com.kshiteesh.datingapp.identityservice.otp.delivery.OtpDeliveryException;
import com.kshiteesh.datingapp.identityservice.otp.delivery.OtpDeliveryProvider;
import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallenge;
import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus;
import com.kshiteesh.datingapp.identityservice.otp.repository.OtpChallengeRepository;
import com.kshiteesh.datingapp.identityservice.phone.PhoneLookupHasher;
import com.kshiteesh.datingapp.identityservice.phone.PhoneNumberNormalizer;
import com.kshiteesh.datingapp.identityservice.ratelimit.RateLimitDecision;
import com.kshiteesh.datingapp.identityservice.ratelimit.RateLimitExceededException;
import com.kshiteesh.datingapp.identityservice.ratelimit.RateLimiter;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OtpChallengeServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");
	private static final String RAW_PHONE = "+1 650-253-0000";
	private static final String NORMALIZED_PHONE = "+16502530000";
	private static final String PHONE_LOOKUP_HASH =
			"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final String OTP = "012345";

	private final PhoneNumberNormalizer phoneNumberNormalizer = org.mockito.Mockito.mock(PhoneNumberNormalizer.class);
	private final PhoneLookupHasher phoneLookupHasher = org.mockito.Mockito.mock(PhoneLookupHasher.class);
	private final OtpGenerator otpGenerator = org.mockito.Mockito.mock(OtpGenerator.class);
	private final OtpProtector otpProtector = new OtpProtector("unit-test-otp-secret");
	private final OtpChallengeRepository otpChallengeRepository = org.mockito.Mockito.mock(OtpChallengeRepository.class);
	private final OtpChallengePersistenceService persistenceService = org.mockito.Mockito.mock(OtpChallengePersistenceService.class);
	private final OtpDeliveryProvider otpDeliveryProvider = org.mockito.Mockito.mock(OtpDeliveryProvider.class);
	private final RateLimiter rateLimiter = org.mockito.Mockito.mock(RateLimiter.class);
	private final OtpProperties otpProperties = new OtpProperties(6, Duration.ofMinutes(5), Duration.ofSeconds(60), 5,
			Duration.ofMinutes(15), 5, "local");
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final OtpChallengeService service = new OtpChallengeService(
			phoneNumberNormalizer,
			phoneLookupHasher,
			otpGenerator,
			otpProtector,
			otpChallengeRepository,
			persistenceService,
			otpDeliveryProvider,
			rateLimiter,
			otpProperties,
			clock);

	@Test
	void requestChallengeCreatesChallengeAndInvokesDeliveryWithoutReturningOtp() {
		UUID challengeId = UUID.randomUUID();
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		stubSuccessfulRequest(challenge);

		OtpChallengeRequestResult result = service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null));

		assertThat(result.challengeId()).isEqualTo(challengeId);
		assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
		assertThat(result.toString()).doesNotContain(OTP);
		verify(otpDeliveryProvider).deliver(NORMALIZED_PHONE, OTP);
		verify(persistenceService).createPendingChallenge(eq(PHONE_LOOKUP_HASH), any(UUID.class), any(String.class), eq(NOW));
		verify(rateLimiter).tryAcquire("otp-request", PHONE_LOOKUP_HASH, 5, Duration.ofMinutes(15), NOW);
	}

	@Test
	void requestChallengeDoesNotPersistRawOtp() {
		UUID challengeId = UUID.randomUUID();
		stubSuccessfulRequest(pendingChallenge(challengeId, OTP));

		service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null));

		verify(persistenceService).createPendingChallenge(eq(PHONE_LOOKUP_HASH), any(UUID.class), org.mockito.ArgumentMatchers.argThat(
				otpHmac -> !OTP.equals(otpHmac) && otpHmac.matches("[0-9a-f]{64}")), eq(NOW));
	}

	@Test
	void requestChallengeDoesNotDependOnIdentityResolution() {
		assertThat(Arrays.stream(OtpChallengeService.class.getDeclaredFields()).map(Field::getType))
				.noneMatch(IdentityResolutionService.class::equals);
	}

	@Test
	void knownAndUnknownPhoneRequestsHaveSameSafeResultShape() {
		OtpChallenge firstChallenge = pendingChallenge(UUID.randomUUID(), OTP);
		OtpChallenge secondChallenge = pendingChallenge(UUID.randomUUID(), OTP);
		stubSuccessfulRequest(firstChallenge);

		OtpChallengeRequestResult known = service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null));
		when(persistenceService.createPendingChallenge(eq(PHONE_LOOKUP_HASH), any(UUID.class), any(String.class), eq(NOW)))
				.thenReturn(secondChallenge);
		OtpChallengeRequestResult unknown = service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null));

		assertThat(known.expiresAt()).isEqualTo(unknown.expiresAt());
		assertThat(known.resendAvailableAt()).isEqualTo(unknown.resendAvailableAt());
	}

	@Test
	void requestChallengeDuringResendCooldownIsRejected() {
		OtpChallenge existingChallenge = pendingChallenge(UUID.randomUUID(), OTP);
		when(phoneNumberNormalizer.normalizeToE164(RAW_PHONE, null)).thenReturn(NORMALIZED_PHONE);
		when(phoneLookupHasher.hash(NORMALIZED_PHONE)).thenReturn(PHONE_LOOKUP_HASH);
		when(rateLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
				.thenReturn(new RateLimitDecision(true, Duration.ZERO));
		when(otpChallengeRepository.findFirstByPhoneNumberHashAndStatusOrderByCreatedAtDesc(
				PHONE_LOOKUP_HASH,
				OtpChallengeStatus.PENDING)).thenReturn(Optional.of(existingChallenge));

		assertThatThrownBy(() -> service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null)))
				.isInstanceOf(ResendCooldownActiveException.class);

		verify(persistenceService, never()).createPendingChallenge(any(), any(), any(), any());
		verify(rateLimiter, never()).tryAcquire(any(), any(), any(Integer.class), any(), any());
	}

	@Test
	void requestLimitWindowAndOtpTtlAreIndependent() {
		OtpChallenge challenge = pendingChallenge(UUID.randomUUID(), OTP);
		stubSuccessfulRequest(challenge);

		service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null));

		verify(rateLimiter).tryAcquire("otp-request", PHONE_LOOKUP_HASH, 5, Duration.ofMinutes(15), NOW);
		assertThat(challenge.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
	}

	@Test
	void rawPhoneNumberIsNotUsedAsRateLimitKey() {
		stubSuccessfulRequest(pendingChallenge(UUID.randomUUID(), OTP));

		service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null));

		verify(rateLimiter).tryAcquire(eq("otp-request"), eq(PHONE_LOOKUP_HASH), any(Integer.class), any(), any());
		verify(rateLimiter, never()).tryAcquire(eq("otp-request"), eq(RAW_PHONE), any(Integer.class), any(), any());
		verify(rateLimiter, never()).tryAcquire(eq("otp-request"), eq(NORMALIZED_PHONE), any(Integer.class), any(), any());
	}

	@Test
	void rateLimitedRequestIsRejected() {
		when(phoneNumberNormalizer.normalizeToE164(RAW_PHONE, null)).thenReturn(NORMALIZED_PHONE);
		when(phoneLookupHasher.hash(NORMALIZED_PHONE)).thenReturn(PHONE_LOOKUP_HASH);
		when(rateLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
				.thenReturn(new RateLimitDecision(false, Duration.ofSeconds(30)));

		assertThatThrownBy(() -> service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null)))
				.isInstanceOf(RateLimitExceededException.class);
	}

	@Test
	void deliveryFailureExpiresPendingChallengeAndDoesNotProduceSuccess() {
		OtpChallenge challenge = pendingChallenge(UUID.randomUUID(), OTP);
		stubSuccessfulRequest(challenge);
		org.mockito.Mockito.doThrow(new OtpDeliveryException("provider unavailable"))
				.when(otpDeliveryProvider).deliver(NORMALIZED_PHONE, OTP);

		assertThatThrownBy(() -> service.requestChallenge(new OtpChallengeRequest(RAW_PHONE, null)))
				.isInstanceOf(OtpDeliveryException.class);

		verify(persistenceService).expirePendingChallenge(challenge.getChallengeId());
	}

	@Test
	void correctOtpBeforeExpirySucceedsAndConsumesChallenge() {
		UUID challengeId = UUID.randomUUID();
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		OtpVerificationResult result = service.verifyChallenge(challengeId, OTP);

		assertThat(result.challengeId()).isEqualTo(challengeId);
		assertThat(result.phoneLookupHash()).isEqualTo(PHONE_LOOKUP_HASH);
		assertThat(result.phoneLookupHash()).isNotEqualTo(NORMALIZED_PHONE);
		assertThat(result.phoneLookupHash()).isNotEqualTo(OTP);
		assertThat(challenge.getStatus()).isEqualTo(OtpChallengeStatus.CONSUMED);
		assertThat(challenge.getConsumedAt()).isEqualTo(NOW);
	}

	@Test
	void incorrectOtpFailsAndIncrementsAttemptCount() {
		UUID challengeId = UUID.randomUUID();
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		assertThatThrownBy(() -> service.verifyChallenge(challengeId, "111111"))
				.isInstanceOf(InvalidOtpException.class);

		assertThat(challenge.getAttemptCount()).isEqualTo(1);
	}

	@Test
	void justBeforeExpiryOtpCanSucceed() {
		UUID challengeId = UUID.randomUUID();
		Clock justBeforeExpiryClock = Clock.fixed(NOW.plus(Duration.ofMinutes(5)).minusMillis(1), ZoneOffset.UTC);
		OtpChallengeService justBeforeExpiryService = serviceWithClock(justBeforeExpiryClock);
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		assertThat(justBeforeExpiryService.verifyChallenge(challengeId, OTP).challengeId()).isEqualTo(challengeId);
	}

	@Test
	void exactlyAtExpiryOtpFails() {
		UUID challengeId = UUID.randomUUID();
		Clock expiryClock = Clock.fixed(NOW.plus(Duration.ofMinutes(5)), ZoneOffset.UTC);
		OtpChallengeService expiryService = serviceWithClock(expiryClock);
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		assertThatThrownBy(() -> expiryService.verifyChallenge(challengeId, OTP))
				.isInstanceOf(OtpChallengeExpiredException.class);
		assertThat(challenge.getStatus()).isEqualTo(OtpChallengeStatus.EXPIRED);
	}

	@Test
	void afterExpiryOtpFails() {
		UUID challengeId = UUID.randomUUID();
		Clock afterExpiryClock = Clock.fixed(NOW.plus(Duration.ofMinutes(5)).plusSeconds(1), ZoneOffset.UTC);
		OtpChallengeService afterExpiryService = serviceWithClock(afterExpiryClock);
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		assertThatThrownBy(() -> afterExpiryService.verifyChallenge(challengeId, OTP))
				.isInstanceOf(OtpChallengeExpiredException.class);
	}

	@Test
	void consumedOtpFailsOnReuse() {
		UUID challengeId = UUID.randomUUID();
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		challenge.markConsumed(NOW.minusSeconds(1));
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		assertThatThrownBy(() -> service.verifyChallenge(challengeId, OTP))
				.isInstanceOf(OtpChallengeConsumedException.class);
	}

	@Test
	void maximumFailedAttemptLimitIsEnforced() {
		UUID challengeId = UUID.randomUUID();
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		for (int index = 0; index < 5; index++) {
			challenge.recordFailedAttempt();
		}
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		assertThatThrownBy(() -> service.verifyChallenge(challengeId, OTP))
				.isInstanceOf(OtpAttemptsExhaustedException.class);
	}

	@Test
	void finalAllowedAttemptCanSucceedBeforeExhaustion() {
		UUID challengeId = UUID.randomUUID();
		OtpChallenge challenge = pendingChallenge(challengeId, OTP);
		for (int index = 0; index < 4; index++) {
			challenge.recordFailedAttempt();
		}
		when(otpChallengeRepository.findByChallengeIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

		OtpVerificationResult result = service.verifyChallenge(challengeId, OTP);

		assertThat(result.challengeId()).isEqualTo(challengeId);
		assertThat(challenge.getStatus()).isEqualTo(OtpChallengeStatus.CONSUMED);
	}

	private void stubSuccessfulRequest(OtpChallenge challenge) {
		when(phoneNumberNormalizer.normalizeToE164(RAW_PHONE, null)).thenReturn(NORMALIZED_PHONE);
		when(phoneLookupHasher.hash(NORMALIZED_PHONE)).thenReturn(PHONE_LOOKUP_HASH);
		when(rateLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
				.thenReturn(new RateLimitDecision(true, Duration.ZERO));
		when(otpChallengeRepository.findFirstByPhoneNumberHashAndStatusOrderByCreatedAtDesc(
				PHONE_LOOKUP_HASH,
				OtpChallengeStatus.PENDING)).thenReturn(Optional.empty());
		when(otpGenerator.generate(6)).thenReturn(OTP);
		when(persistenceService.createPendingChallenge(eq(PHONE_LOOKUP_HASH), any(UUID.class), any(String.class), eq(NOW)))
				.thenReturn(challenge);
	}

	private OtpChallenge pendingChallenge(UUID challengeId, String otp) {
		return OtpChallenge.pending(
				challengeId,
				PHONE_LOOKUP_HASH,
				otpProtector.protect(challengeId, otp),
				NOW,
				NOW.plus(Duration.ofMinutes(5)),
				NOW.plus(Duration.ofSeconds(60)),
				5);
	}

	private OtpChallengeService serviceWithClock(Clock testClock) {
		return new OtpChallengeService(
				phoneNumberNormalizer,
				phoneLookupHasher,
				otpGenerator,
				otpProtector,
				otpChallengeRepository,
				persistenceService,
				otpDeliveryProvider,
				rateLimiter,
				otpProperties,
				testClock);
	}
}
