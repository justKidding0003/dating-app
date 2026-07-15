package com.kshiteesh.datingapp.identityservice.auth.api;

import com.kshiteesh.datingapp.identityservice.otp.delivery.OtpDeliveryException;
import com.kshiteesh.datingapp.identityservice.otp.service.InvalidOtpException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpAttemptsExhaustedException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeConsumedException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeExpiredException;
import com.kshiteesh.datingapp.identityservice.otp.service.OtpChallengeNotFoundException;
import com.kshiteesh.datingapp.identityservice.otp.service.ResendCooldownActiveException;
import com.kshiteesh.datingapp.identityservice.phone.InvalidPhoneNumberException;
import com.kshiteesh.datingapp.identityservice.ratelimit.RateLimitExceededException;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshTokenException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

	private final Clock clock;

	public AuthExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(AuthExceptionHandler::validationMessage)
				.orElse("Request validation failed.");
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
	}

	@ExceptionHandler(InvalidPhoneNumberException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidPhoneNumber(InvalidPhoneNumberException ex) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_PHONE_NUMBER", "Phone number is invalid.");
	}

	@ExceptionHandler({InvalidOtpException.class, OtpChallengeNotFoundException.class})
	ResponseEntity<ApiErrorResponse> handleInvalidOtp(RuntimeException ex) {
		return error(HttpStatus.UNAUTHORIZED, "OTP_VERIFICATION_FAILED", "OTP verification failed.");
	}

	@ExceptionHandler({
			OtpChallengeExpiredException.class,
			OtpChallengeConsumedException.class,
			OtpAttemptsExhaustedException.class
	})
	ResponseEntity<ApiErrorResponse> handleOtpUnavailable(RuntimeException ex) {
		return error(HttpStatus.BAD_REQUEST, "OTP_CHALLENGE_UNAVAILABLE", "OTP challenge cannot be used.");
	}

	@ExceptionHandler({RateLimitExceededException.class, ResendCooldownActiveException.class})
	ResponseEntity<ApiErrorResponse> handleRateLimit(RuntimeException ex) {
		return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Request is temporarily rate limited.");
	}

	@ExceptionHandler(RefreshTokenException.class)
	ResponseEntity<ApiErrorResponse> handleRefreshToken(RefreshTokenException ex) {
		return error(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "Refresh token cannot be used.");
	}

	@ExceptionHandler(OtpDeliveryException.class)
	ResponseEntity<ApiErrorResponse> handleOtpDelivery(OtpDeliveryException ex) {
		return error(HttpStatus.SERVICE_UNAVAILABLE, "OTP_DELIVERY_FAILED", "OTP delivery is temporarily unavailable.");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request is invalid.");
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(Instant.now(clock), status.value(), code, message));
	}

	private static String validationMessage(FieldError fieldError) {
		return fieldError.getField() + " is invalid.";
	}
}
