package com.kshiteesh.datingapp.identityservice.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "identities")
public class Identity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID identityId;

	@Column(name = "phone_number_hash", nullable = false, unique = true, length = 128)
	private String phoneNumberHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private IdentityStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Identity() {
	}

	private Identity(UUID identityId, String phoneNumberHash, IdentityStatus status) {
		this.identityId = Objects.requireNonNull(identityId, "identityId must not be null");
		this.phoneNumberHash = requireLookupHash(phoneNumberHash);
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public static Identity active(String phoneNumberHash) {
		return new Identity(UUID.randomUUID(), phoneNumberHash, IdentityStatus.ACTIVE);
	}

	public UUID getIdentityId() {
		return identityId;
	}

	public String getPhoneNumberHash() {
		return phoneNumberHash;
	}

	public IdentityStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	private static String requireLookupHash(String phoneNumberHash) {
		if (phoneNumberHash == null || phoneNumberHash.isBlank()) {
			throw new IllegalArgumentException("phoneNumberHash must not be blank");
		}
		return phoneNumberHash;
	}
}
