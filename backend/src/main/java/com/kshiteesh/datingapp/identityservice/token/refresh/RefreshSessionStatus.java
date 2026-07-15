package com.kshiteesh.datingapp.identityservice.token.refresh;

public enum RefreshSessionStatus {
	ACTIVE,
	ROTATED,
	REVOKED,
	EXPIRED,
	REUSE_DETECTED
}
