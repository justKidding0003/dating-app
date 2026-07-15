package com.kshiteesh.datingapp.identityservice.auth.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kshiteesh.datingapp.identityservice.auth.service.AuthenticationService;
import com.kshiteesh.datingapp.identityservice.otp.service.InvalidOtpException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeConsumedException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeExpiredException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeRequestResult;
import com.kshiteesh.datingapp.identityservice.security.SecurityConfiguration;
import com.kshiteesh.datingapp.identityservice.token.TokenIssueResult;
import com.kshiteesh.datingapp.identityservice.token.access.AccessToken;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshToken;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshTokenException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({AuthExceptionHandler.class, SecurityConfiguration.class, AuthControllerTest.TestClockConfiguration.class})
class AuthControllerTest {

	private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");
	private static final UUID CHALLENGE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;

	@MockitoBean
	private AuthenticationService authenticationService;

	@Autowired
	AuthControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
	}

	@Test
	void otpRequestReturnsSafeChallengeData() throws Exception {
		when(authenticationService.requestOtp("+16502530000", "US")).thenReturn(new OtpChallengeRequestResult(
				CHALLENGE_ID,
				NOW.plusSeconds(300),
				NOW.plusSeconds(60)));

		mockMvc.perform(post("/api/v1/auth/otp/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("phoneNumber", "+16502530000", "regionCode", "US"))))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.challengeId").value(CHALLENGE_ID.toString()))
				.andExpect(jsonPath("$.expiresAt").value("2026-07-09T00:05:00Z"))
				.andExpect(jsonPath("$.resendAvailableAt").value("2026-07-09T00:01:00Z"))
				.andExpect(content().string(not(containsString("phone"))))
				.andExpect(content().string(not(containsString("hash"))))
				.andExpect(content().string(not(containsString("012345"))));
	}

	@Test
	void otpRequestValidationFailureReturnsSafeError() throws Exception {
		mockMvc.perform(post("/api/v1/auth/otp/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("regionCode", "US"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.message").value("phoneNumber is invalid."))
				.andExpect(content().string(not(containsString("stackTrace"))));
	}

	@Test
	void otpVerifyReturnsTokenPair() throws Exception {
		when(authenticationService.verifyOtpAndIssueTokens(CHALLENGE_ID, "012345", "device-1", "Pixel"))
				.thenReturn(tokenIssueResult());

		mockMvc.perform(post("/api/v1/auth/otp/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of(
								"challengeId", CHALLENGE_ID.toString(),
								"otp", "012345",
								"deviceId", "device-1",
								"deviceName", "Pixel"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.accessTokenExpiresAt").value("2026-07-09T00:15:00Z"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.refreshTokenExpiresAt").value("2026-08-08T00:00:00Z"));
	}

	@Test
	void invalidOtpReturnsGenericVerificationFailure() throws Exception {
		when(authenticationService.verifyOtpAndIssueTokens(CHALLENGE_ID, "111111", null, null))
				.thenThrow(new InvalidOtpException());

		mockMvc.perform(post("/api/v1/auth/otp/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("challengeId", CHALLENGE_ID.toString(), "otp", "111111"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("OTP_VERIFICATION_FAILED"))
				.andExpect(jsonPath("$.message").value("OTP verification failed."));
	}

	@Test
	void expiredOrConsumedOtpReturnsSameUnavailableError() throws Exception {
		when(authenticationService.verifyOtpAndIssueTokens(CHALLENGE_ID, "012345", null, null))
				.thenThrow(new OtpChallengeExpiredException());

		mockMvc.perform(post("/api/v1/auth/otp/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("challengeId", CHALLENGE_ID.toString(), "otp", "012345"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("OTP_CHALLENGE_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("OTP challenge cannot be used."));

		when(authenticationService.verifyOtpAndIssueTokens(CHALLENGE_ID, "654321", null, null))
				.thenThrow(new OtpChallengeConsumedException());

		mockMvc.perform(post("/api/v1/auth/otp/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("challengeId", CHALLENGE_ID.toString(), "otp", "654321"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("OTP_CHALLENGE_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("OTP challenge cannot be used."));
	}

	@Test
	void refreshRotatesAndReturnsNewTokenPair() throws Exception {
		when(authenticationService.refreshTokens("old-refresh-token")).thenReturn(tokenIssueResult());

		mockMvc.perform(post("/api/v1/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("refreshToken", "old-refresh-token"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"));
	}

	@Test
	void oldOrRevokedRefreshTokenReturnsSafeError() throws Exception {
		when(authenticationService.refreshTokens("old-refresh-token"))
				.thenThrow(new RefreshTokenException("Refresh token cannot be used."));

		mockMvc.perform(post("/api/v1/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("refreshToken", "old-refresh-token"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
				.andExpect(jsonPath("$.message").value("Refresh token cannot be used."));
	}

	@Test
	void logoutRevokesRefreshSession() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("refreshToken", "refresh-token"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("success"));

		verify(authenticationService).logout("refresh-token");
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}

	private static TokenIssueResult tokenIssueResult() {
		return new TokenIssueResult(
				new AccessToken("access-token", UUID.randomUUID(), NOW, NOW.plusSeconds(900)),
				new RefreshToken("refresh-token", UUID.randomUUID(), NOW.plusSeconds(2_592_000)));
	}

	@TestConfiguration
	static class TestClockConfiguration {

		@Bean
		Clock clock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}
}
