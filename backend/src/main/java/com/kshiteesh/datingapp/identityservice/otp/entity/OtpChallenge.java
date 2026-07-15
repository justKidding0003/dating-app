package com.kshiteesh.datingapp.identityservice.otp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

	@Id
	@Column(name = "id", nullable = false)
	private UUID challengeId;

	@Column(name = "identity_id")
	private UUID identityId;

	@Column(name = "phone_number_hash", nullable = false, length = 128)
	private String phoneNumberHash;

	@Column(name = "otp_hmac", nullable = false, length = 128)
	private String otpHmac;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private OtpChallengeStatus status;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "resend_available_at", nullable = false)
	private Instant resendAvailableAt;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "max_attempts", nullable = false)
	private int maxAttempts;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	protected OtpChallenge() {
	}

	private OtpChallenge(
			UUID challengeId,
			String phoneNumberHash,
			String otpHmac,
			Instant createdAt,
			Instant expiresAt,
			Instant resendAvailableAt,
			int maxAttempts) {
		this.challengeId = Objects.requireNonNull(challengeId, "challengeId must not be null");
		this.phoneNumberHash = requireText(phoneNumberHash, "phoneNumberHash");
		this.otpHmac = requireText(otpHmac, "otpHmac");
		this.status = OtpChallengeStatus.PENDING;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		this.resendAvailableAt = Objects.requireNonNull(resendAvailableAt, "resendAvailableAt must not be null");
		if (maxAttempts <= 0) {
			throw new IllegalArgumentException("maxAttempts must be positive");
		}
		this.maxAttempts = maxAttempts;
	}

	public static OtpChallenge pending(
			UUID challengeId,
			String phoneNumberHash,
			String otpHmac,
			Instant createdAt,
			Instant expiresAt,
			Instant resendAvailableAt,
			int maxAttempts) {
		return new OtpChallenge(challengeId, phoneNumberHash, otpHmac, createdAt, expiresAt, resendAvailableAt,
				maxAttempts);
	}

	public UUID getChallengeId() {
		return challengeId;
	}

	public UUID getIdentityId() {
		return identityId;
	}

	public String getPhoneNumberHash() {
		return phoneNumberHash;
	}

	public String getOtpHmac() {
		return otpHmac;
	}

	public OtpChallengeStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getResendAvailableAt() {
		return resendAvailableAt;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public boolean isPending() {
		return status == OtpChallengeStatus.PENDING;
	}

	public boolean isExpiredAt(Instant now) {
		return !now.isBefore(expiresAt);
	}

	public boolean hasExhaustedAttempts() {
		return attemptCount >= maxAttempts;
	}

	public void recordFailedAttempt() {
		if (attemptCount < maxAttempts) {
			attemptCount++;
		}
		if (attemptCount >= maxAttempts) {
			status = OtpChallengeStatus.LOCKED;
		}
	}

	public void markConsumed(Instant verifiedAt) {
		this.verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
		this.consumedAt = verifiedAt;
		this.status = OtpChallengeStatus.CONSUMED;
	}

	public void markExpired() {
		if (status == OtpChallengeStatus.PENDING) {
			status = OtpChallengeStatus.EXPIRED;
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
