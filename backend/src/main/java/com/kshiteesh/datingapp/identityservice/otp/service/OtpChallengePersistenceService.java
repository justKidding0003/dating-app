package com.kshiteesh.datingapp.identityservice.otp.service;

import com.kshiteesh.datingapp.identityservice.otp.OtpProperties;
import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallenge;
import com.kshiteesh.datingapp.identityservice.otp.repository.OtpChallengeRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OtpChallengePersistenceService {

	private final OtpChallengeRepository otpChallengeRepository;
	private final OtpProperties otpProperties;

	OtpChallengePersistenceService(OtpChallengeRepository otpChallengeRepository, OtpProperties otpProperties) {
		this.otpChallengeRepository = otpChallengeRepository;
		this.otpProperties = otpProperties;
	}

	@Transactional
	OtpChallenge createPendingChallenge(String phoneLookupHash, UUID challengeId, String otpHmac, Instant now) {
		OtpChallenge challenge = OtpChallenge.pending(
				challengeId,
				phoneLookupHash,
				otpHmac,
				now,
				now.plus(otpProperties.ttl()),
				now.plus(otpProperties.resendCooldown()),
				otpProperties.maxVerificationAttempts());
		OtpChallenge savedChallenge = otpChallengeRepository.saveAndFlush(challenge);
		otpChallengeRepository.expireOtherPendingChallenges(phoneLookupHash, challengeId);
		return savedChallenge;
	}

	@Transactional
	void expirePendingChallenge(UUID challengeId) {
		otpChallengeRepository.expirePendingChallenge(challengeId);
	}
}
