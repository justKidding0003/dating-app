package com.kshiteesh.datingapp.identityservice.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kshiteesh.datingapp.identityservice.identity.entity.Identity;
import com.kshiteesh.datingapp.identityservice.identity.service.IdentityResolutionService;
import com.kshiteesh.datingapp.identityservice.otp.service.InvalidOtpException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeRequestResult;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeService;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpVerificationResult;
import com.kshiteesh.datingapp.identityservice.token.TokenIssueResult;
import com.kshiteesh.datingapp.identityservice.token.TokenOrchestrationService;
import com.kshiteesh.datingapp.identityservice.token.access.AccessToken;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshSessionService;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshToken;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

	private static final UUID CHALLENGE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
	private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");
	private static final String PHONE_LOOKUP_HASH =
			"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private final OtpChallengeService otpChallengeService = org.mockito.Mockito.mock(OtpChallengeService.class);
	private final IdentityResolutionService identityResolutionService = org.mockito.Mockito.mock(IdentityResolutionService.class);
	private final TokenOrchestrationService tokenOrchestrationService = org.mockito.Mockito.mock(TokenOrchestrationService.class);
	private final RefreshSessionService refreshSessionService = org.mockito.Mockito.mock(RefreshSessionService.class);
	private final AuthenticationService service = new AuthenticationService(
			otpChallengeService,
			identityResolutionService,
			tokenOrchestrationService,
			refreshSessionService);

	@Test
	void requestOtpDelegatesToExistingOtpChallengeFlow() {
		OtpChallengeRequestResult expected = new OtpChallengeRequestResult(
				CHALLENGE_ID,
				NOW.plusSeconds(300),
				NOW.plusSeconds(60));
		when(otpChallengeService.requestChallenge(org.mockito.ArgumentMatchers.argThat(request ->
				"+16502530000".equals(request.rawPhoneNumber()) && "US".equals(request.regionCode()))))
				.thenReturn(expected);

		OtpChallengeRequestResult result = service.requestOtp("+16502530000", "US");

		assertThat(result).isEqualTo(expected);
	}

	@Test
	void successfulOtpVerificationResolvesIdentityAndIssuesTokens() {
		Identity identity = Identity.active(PHONE_LOOKUP_HASH);
		TokenIssueResult expected = tokenIssueResult();
		when(otpChallengeService.verifyChallenge(CHALLENGE_ID, "012345"))
				.thenReturn(new OtpVerificationResult(CHALLENGE_ID, PHONE_LOOKUP_HASH, NOW));
		when(identityResolutionService.getOrCreateAfterSuccessfulAuthentication(PHONE_LOOKUP_HASH)).thenReturn(identity);
		when(tokenOrchestrationService.issueAfterSuccessfulAuthentication(
				identity.getIdentityId(),
				"device-1",
				"Pixel")).thenReturn(expected);

		TokenIssueResult result = service.verifyOtpAndIssueTokens(CHALLENGE_ID, "012345", "device-1", "Pixel");

		assertThat(result).isEqualTo(expected);
		verify(identityResolutionService).getOrCreateAfterSuccessfulAuthentication(PHONE_LOOKUP_HASH);
		verify(tokenOrchestrationService).issueAfterSuccessfulAuthentication(identity.getIdentityId(), "device-1", "Pixel");
	}

	@Test
	void invalidOtpDoesNotResolveIdentityOrIssueTokens() {
		when(otpChallengeService.verifyChallenge(CHALLENGE_ID, "111111")).thenThrow(new InvalidOtpException());

		assertThatThrownBy(() -> service.verifyOtpAndIssueTokens(CHALLENGE_ID, "111111", "device-1", "Pixel"))
				.isInstanceOf(InvalidOtpException.class);

		verify(identityResolutionService, never()).getOrCreateAfterSuccessfulAuthentication(org.mockito.ArgumentMatchers.any());
		verify(tokenOrchestrationService, never()).issueAfterSuccessfulAuthentication(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void refreshDelegatesToTokenRotation() {
		TokenIssueResult expected = tokenIssueResult();
		when(tokenOrchestrationService.rotate("refresh-token")).thenReturn(expected);

		assertThat(service.refreshTokens("refresh-token")).isEqualTo(expected);
	}

	@Test
	void logoutDelegatesToRefreshSessionRevocationByToken() {
		service.logout("refresh-token");

		verify(refreshSessionService).logout("refresh-token");
	}

	private static TokenIssueResult tokenIssueResult() {
		return new TokenIssueResult(
				new AccessToken("access-token", UUID.randomUUID(), NOW, NOW.plusSeconds(900)),
				new RefreshToken("refresh-token", UUID.randomUUID(), NOW.plusSeconds(2_592_000)));
	}
}
