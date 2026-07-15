package com.kshiteesh.datingapp.identityservice.otp.service;

import com.kshiteesh.datingapp.identityservice.otp.OtpProperties;
import com.kshiteesh.datingapp.identityservice.otp.delivery.OtpDeliveryException;
import com.kshiteesh.datingapp.identityservice.otp.delivery.OtpDeliveryProvider;
import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallenge;
import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus;
import com.kshiteesh.datingapp.identityservice.otp.repository.OtpChallengeRepository;
import com.kshiteesh.datingapp.identityservice.phone.PhoneLookupHasher;
import com.kshiteesh.datingapp.identityservice.phone.PhoneNumberNormalizer;
import com.kshiteesh.datingapp.identityservice.ratelimit.RateLimitDecision;
import com.kshiteesh.datingapp.identityservice.ratelimit.RateLimiter;
import com.kshiteesh.datingapp.identityservice.ratelimit.RateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpChallengeService {

	private final PhoneNumberNormalizer phoneNumberNormalizer;
	private final PhoneLookupHasher phoneLookupHasher;
	private final OtpGenerator otpGenerator;
	private final OtpProtector otpProtector;
	private final OtpChallengeRepository otpChallengeRepository;
	private final OtpChallengePersistenceService otpChallengePersistenceService;
	private final OtpDeliveryProvider otpDeliveryProvider;
	private final RateLimiter rateLimiter;
	private final OtpProperties otpProperties;
	private final Clock clock;

	public OtpChallengeService(
			PhoneNumberNormalizer phoneNumberNormalizer,
			PhoneLookupHasher phoneLookupHasher,
			OtpGenerator otpGenerator,
			OtpProtector otpProtector,
			OtpChallengeRepository otpChallengeRepository,
			OtpChallengePersistenceService otpChallengePersistenceService,
			OtpDeliveryProvider otpDeliveryProvider,
			RateLimiter rateLimiter,
			OtpProperties otpProperties,
			Clock clock) {
		this.phoneNumberNormalizer = phoneNumberNormalizer;
		this.phoneLookupHasher = phoneLookupHasher;
		this.otpGenerator = otpGenerator;
		this.otpProtector = otpProtector;
		this.otpChallengeRepository = otpChallengeRepository;
		this.otpChallengePersistenceService = otpChallengePersistenceService;
		this.otpDeliveryProvider = otpDeliveryProvider;
		this.rateLimiter = rateLimiter;
		this.otpProperties = otpProperties;
		this.clock = clock;
	}

	public OtpChallengeRequestResult requestChallenge(OtpChallengeRequest request) {
		String normalizedPhoneNumber = phoneNumberNormalizer.normalizeToE164(request.rawPhoneNumber(), request.regionCode());
		String phoneLookupHash = phoneLookupHasher.hash(normalizedPhoneNumber);
		Instant now = Instant.now(clock);

		enforceResendCooldown(phoneLookupHash, now);
		enforceRequestRateLimit(phoneLookupHash, now);

		UUID challengeId = UUID.randomUUID();
		String otp = otpGenerator.generate(otpProperties.length());
		String otpHmac = otpProtector.protect(challengeId, otp);
		OtpChallenge challenge = otpChallengePersistenceService.createPendingChallenge(phoneLookupHash, challengeId, otpHmac, now);

		try {
			otpDeliveryProvider.deliver(normalizedPhoneNumber, otp);
		} catch (RuntimeException ex) {
			otpChallengePersistenceService.expirePendingChallenge(challenge.getChallengeId());
			throw new OtpDeliveryException("OTP delivery failed.", ex);
		}

		return new OtpChallengeRequestResult(
				challenge.getChallengeId(),
				challenge.getExpiresAt(),
				challenge.getResendAvailableAt());
	}

	@Transactional
	public OtpVerificationResult verifyChallenge(UUID challengeId, String submittedOtp) {
		OtpChallenge challenge = otpChallengeRepository.findByChallengeIdForUpdate(challengeId)
				.orElseThrow(OtpChallengeNotFoundException::new);
		Instant now = Instant.now(clock);

		ensureVerifiable(challenge, now);

		if (!otpProtector.matches(challenge.getChallengeId(), submittedOtp, challenge.getOtpHmac())) {
			challenge.recordFailedAttempt();
			throw new InvalidOtpException();
		}

		challenge.markConsumed(now);
		return new OtpVerificationResult(challenge.getChallengeId(), challenge.getPhoneNumberHash(), now);
	}

	private void enforceRequestRateLimit(String phoneLookupHash, Instant now) {
		RateLimitDecision decision = rateLimiter.tryAcquire(
				"otp-request",
				phoneLookupHash,
				otpProperties.requestLimit(),
				otpProperties.requestLimitWindow(),
				now);
		if (!decision.allowed()) {
			throw new RateLimitExceededException("OTP request rate limit exceeded.", decision.retryAfter());
		}
	}

	private void enforceResendCooldown(String phoneLookupHash, Instant now) {
		otpChallengeRepository.findFirstByPhoneNumberHashAndStatusOrderByCreatedAtDesc(
						phoneLookupHash,
						OtpChallengeStatus.PENDING)
				.filter(challenge -> now.isBefore(challenge.getResendAvailableAt()))
				.ifPresent(challenge -> {
					throw new ResendCooldownActiveException(challenge.getResendAvailableAt());
				});
	}

	private static void ensureVerifiable(OtpChallenge challenge, Instant now) {
		if (!challenge.isPending()) {
			if (challenge.getStatus() == OtpChallengeStatus.LOCKED) {
				throw new OtpAttemptsExhaustedException();
			}
			throw new OtpChallengeConsumedException();
		}
		if (challenge.isExpiredAt(now)) {
			challenge.markExpired();
			throw new OtpChallengeExpiredException();
		}
		if (challenge.hasExhaustedAttempts()) {
			challenge.recordFailedAttempt();
			throw new OtpAttemptsExhaustedException();
		}
	}
}
