package com.kshiteesh.datingapp.identityservice.auth.service;

import com.kshiteesh.datingapp.identityservice.identity.entity.Identity;
import com.kshiteesh.datingapp.identityservice.identity.service.IdentityResolutionService;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeRequest;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeRequestResult;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeService;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpVerificationResult;
import com.kshiteesh.datingapp.identityservice.token.TokenIssueResult;
import com.kshiteesh.datingapp.identityservice.token.TokenOrchestrationService;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private final OtpChallengeService otpChallengeService;
	private final IdentityResolutionService identityResolutionService;
	private final TokenOrchestrationService tokenOrchestrationService;
	private final RefreshSessionService refreshSessionService;

	public AuthenticationService(
			OtpChallengeService otpChallengeService,
			IdentityResolutionService identityResolutionService,
			TokenOrchestrationService tokenOrchestrationService,
			RefreshSessionService refreshSessionService) {
		this.otpChallengeService = otpChallengeService;
		this.identityResolutionService = identityResolutionService;
		this.tokenOrchestrationService = tokenOrchestrationService;
		this.refreshSessionService = refreshSessionService;
	}

	public OtpChallengeRequestResult requestOtp(String phoneNumber, String regionCode) {
		return otpChallengeService.requestChallenge(new OtpChallengeRequest(phoneNumber, regionCode));
	}

	@Transactional
	public TokenIssueResult verifyOtpAndIssueTokens(
			java.util.UUID challengeId,
			String otp,
			String deviceId,
			String deviceName) {
		OtpVerificationResult verificationResult = otpChallengeService.verifyChallenge(challengeId, otp);
		Identity identity = identityResolutionService.getOrCreateAfterSuccessfulAuthentication(
				verificationResult.phoneLookupHash());
		return tokenOrchestrationService.issueAfterSuccessfulAuthentication(
				identity.getIdentityId(),
				deviceId,
				deviceName);
	}

	public TokenIssueResult refreshTokens(String refreshToken) {
		return tokenOrchestrationService.rotate(refreshToken);
	}

	public void logout(String refreshToken) {
		refreshSessionService.logout(refreshToken);
	}
}
