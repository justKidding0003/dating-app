package com.kshiteesh.datingapp.identityservice.token.refresh;

import com.kshiteesh.datingapp.identityservice.security.SecurityProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshSessionService {

	private final RefreshSessionRepository refreshSessionRepository;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenProtector refreshTokenProtector;
	private final SecurityProperties securityProperties;
	private final Clock clock;

	public RefreshSessionService(
			RefreshSessionRepository refreshSessionRepository,
			RefreshTokenGenerator refreshTokenGenerator,
			RefreshTokenProtector refreshTokenProtector,
			SecurityProperties securityProperties,
			Clock clock) {
		this.refreshSessionRepository = refreshSessionRepository;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.refreshTokenProtector = refreshTokenProtector;
		this.securityProperties = securityProperties;
		this.clock = clock;
		if (securityProperties.refreshTokenTtl() == null || securityProperties.refreshTokenTtl().isZero()
				|| securityProperties.refreshTokenTtl().isNegative()) {
			throw new RefreshTokenException("Refresh token TTL must be positive.");
		}
	}

	@Transactional
	public RefreshToken createSession(UUID identityId, String deviceId, String deviceName) {
		return createSession(identityId, null, deviceId, deviceName);
	}

	@Transactional
	public RefreshRotationResult rotate(String presentedRefreshToken) {
		String presentedHash = refreshTokenProtector.protect(presentedRefreshToken);
		RefreshSession session = refreshSessionRepository.findByRefreshTokenHashForUpdate(presentedHash)
				.orElseThrow(() -> new RefreshTokenException("Refresh token is invalid."));
		Instant now = Instant.now(clock);
		if (session.getStatus() != RefreshSessionStatus.ACTIVE) {
			recordReuse(session, presentedHash, now);
			throw new RefreshTokenException("Refresh token cannot be used.");
		}
		if (session.isExpiredAt(now)) {
			session.markExpired(now);
			throw new RefreshTokenException("Refresh token has expired.");
		}
		session.markRotated(now);
		RefreshToken newRefreshToken = createSession(
				session.getIdentityId(),
				presentedHash,
				session.getDeviceId(),
				session.getDeviceName());
		return new RefreshRotationResult(session.getIdentityId(), newRefreshToken);
	}

	@Transactional
	public void logout(UUID refreshSessionId) {
		RefreshSession session = refreshSessionRepository.findBySessionIdForUpdate(refreshSessionId)
				.orElseThrow(() -> new RefreshTokenException("Refresh session was not found."));
		if (session.getStatus() == RefreshSessionStatus.ACTIVE) {
			session.markRevoked(Instant.now(clock));
		}
	}

	@Transactional
	public void logout(String presentedRefreshToken) {
		String presentedHash = refreshTokenProtector.protect(presentedRefreshToken);
		RefreshSession session = refreshSessionRepository.findByRefreshTokenHashForUpdate(presentedHash)
				.orElseThrow(() -> new RefreshTokenException("Refresh token is invalid."));
		if (session.getStatus() == RefreshSessionStatus.ACTIVE) {
			session.markRevoked(Instant.now(clock));
		}
	}

	private RefreshToken createSession(UUID identityId, String previousTokenHash, String deviceId, String deviceName) {
		Instant now = Instant.now(clock);
		String rawToken = refreshTokenGenerator.generate();
		String tokenHash = refreshTokenProtector.protect(rawToken);
		RefreshSession session = RefreshSession.active(
				identityId,
				tokenHash,
				previousTokenHash,
				now,
				now.plus(securityProperties.refreshTokenTtl()),
				deviceId,
				deviceName);
		RefreshSession savedSession = refreshSessionRepository.saveAndFlush(session);
		return new RefreshToken(rawToken, savedSession.getSessionId(), savedSession.getExpiresAt());
	}

	private void recordReuse(RefreshSession session, String presentedHash, Instant now) {
		session.markReuseDetected(now);
		refreshSessionRepository.findActiveSessionsByPreviousTokenHashForUpdate(presentedHash)
				.forEach(activeChildSession -> activeChildSession.markRevoked(now));
	}
}
