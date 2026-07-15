package com.kshiteesh.datingapp.identityservice.token.refresh;

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
@Table(name = "refresh_sessions")
public class RefreshSession {

	@Id
	@Column(name = "id", nullable = false)
	private UUID sessionId;

	@Column(name = "identity_id", nullable = false)
	private UUID identityId;

	@Column(name = "refresh_token_hash", nullable = false, unique = true, length = 128)
	private String refreshTokenHash;

	@Column(name = "previous_token_hash", length = 128)
	private String previousTokenHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private RefreshSessionStatus status;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "rotated_at")
	private Instant rotatedAt;

	@Column(name = "reuse_detected_at")
	private Instant reuseDetectedAt;

	@Column(name = "device_id", length = 128)
	private String deviceId;

	@Column(name = "device_name", length = 128)
	private String deviceName;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RefreshSession() {
	}

	private RefreshSession(UUID sessionId, UUID identityId, String refreshTokenHash, String previousTokenHash,
			Instant now, Instant expiresAt, String deviceId, String deviceName) {
		this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
		this.identityId = Objects.requireNonNull(identityId, "identityId must not be null");
		this.refreshTokenHash = requireText(refreshTokenHash, "refreshTokenHash");
		this.previousTokenHash = previousTokenHash;
		this.status = RefreshSessionStatus.ACTIVE;
		this.createdAt = Objects.requireNonNull(now, "createdAt must not be null");
		this.updatedAt = now;
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		this.deviceId = deviceId;
		this.deviceName = deviceName;
	}

	public static RefreshSession active(UUID identityId, String refreshTokenHash, String previousTokenHash,
			Instant now, Instant expiresAt, String deviceId, String deviceName) {
		return new RefreshSession(UUID.randomUUID(), identityId, refreshTokenHash, previousTokenHash, now, expiresAt,
				deviceId, deviceName);
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public UUID getIdentityId() {
		return identityId;
	}

	public String getRefreshTokenHash() {
		return refreshTokenHash;
	}

	public String getPreviousTokenHash() {
		return previousTokenHash;
	}

	public RefreshSessionStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getRotatedAt() {
		return rotatedAt;
	}

	public Instant getReuseDetectedAt() {
		return reuseDetectedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public boolean isExpiredAt(Instant now) {
		return !now.isBefore(expiresAt);
	}

	public void markRotated(Instant now) {
		status = RefreshSessionStatus.ROTATED;
		rotatedAt = now;
		updatedAt = now;
	}

	public void markRevoked(Instant now) {
		status = RefreshSessionStatus.REVOKED;
		revokedAt = now;
		updatedAt = now;
	}

	public void markExpired(Instant now) {
		status = RefreshSessionStatus.EXPIRED;
		updatedAt = now;
	}

	public void markReuseDetected(Instant now) {
		status = RefreshSessionStatus.REUSE_DETECTED;
		reuseDetectedAt = now;
		updatedAt = now;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
