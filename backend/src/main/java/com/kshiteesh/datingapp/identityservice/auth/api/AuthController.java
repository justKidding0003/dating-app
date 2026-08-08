package com.kshiteesh.datingapp.identityservice.auth.api;

import com.kshiteesh.datingapp.identityservice.auth.service.AuthenticationService;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeRequestResult;
import com.kshiteesh.datingapp.identityservice.token.TokenIssueResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthenticationService authenticationService;

	public AuthController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/otp/request")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public OtpRequestHttpResponse requestOtp(@Valid @RequestBody OtpRequestHttpRequest request) {
		OtpChallengeRequestResult result = authenticationService.requestOtp(request.phoneNumber(), request.regionCode());
		return new OtpRequestHttpResponse(result.challengeId(), result.expiresAt(), result.resendAvailableAt());
	}

	@PostMapping("/otp/verify")
	public TokenHttpResponse verifyOtp(@Valid @RequestBody OtpVerifyHttpRequest request) {
		return toTokenResponse(authenticationService.verifyOtpAndIssueTokens(
				request.challengeId(),
				request.otp(),
				request.deviceId(),
				request.deviceName()));
	}

	@PostMapping("/token/refresh")
	public TokenHttpResponse refresh(@Valid @RequestBody RefreshTokenHttpRequest request) {
		return toTokenResponse(authenticationService.refreshTokens(request.refreshToken()));
	}

	@PostMapping("/logout")
	public LogoutHttpResponse logout(@Valid @RequestBody RefreshTokenHttpRequest request) {
		authenticationService.logout(request.refreshToken());
		return new LogoutHttpResponse("success");
	}

	private static TokenHttpResponse toTokenResponse(TokenIssueResult result) {
		return new TokenHttpResponse(
				result.accessToken().value(),
				result.accessToken().expiresAt(),
				result.refreshToken().value(),
				result.refreshToken().expiresAt());
	}
}
