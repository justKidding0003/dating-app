CREATE TABLE identities (
    id UUID PRIMARY KEY,
    phone_number_hash VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_identities_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE otp_challenges (
    id UUID PRIMARY KEY,
    identity_id UUID NULL REFERENCES identities (id),
    phone_number_hash VARCHAR(128) NOT NULL,
    otp_hmac VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    resend_available_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ NULL,
    consumed_at TIMESTAMPTZ NULL,
    CONSTRAINT chk_otp_challenges_status CHECK (status IN ('PENDING', 'VERIFIED', 'CONSUMED', 'EXPIRED', 'LOCKED')),
    CONSTRAINT chk_otp_challenges_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT chk_otp_challenges_max_attempts CHECK (max_attempts > 0)
);

CREATE INDEX idx_otp_challenges_phone_status_created_at
    ON otp_challenges (phone_number_hash, status, created_at DESC);

CREATE INDEX idx_otp_challenges_expires_at
    ON otp_challenges (expires_at);

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    identity_id UUID NOT NULL REFERENCES identities (id),
    refresh_token_hash VARCHAR(128) NOT NULL UNIQUE,
    previous_token_hash VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    rotated_at TIMESTAMPTZ NULL,
    reuse_detected_at TIMESTAMPTZ NULL,
    device_id VARCHAR(128) NULL,
    device_name VARCHAR(128) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_refresh_sessions_status CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED', 'REUSE_DETECTED'))
);

CREATE INDEX idx_refresh_sessions_identity_status
    ON refresh_sessions (identity_id, status);

CREATE INDEX idx_refresh_sessions_expires_at
    ON refresh_sessions (expires_at);
