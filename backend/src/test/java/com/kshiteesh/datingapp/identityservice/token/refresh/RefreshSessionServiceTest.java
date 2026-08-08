package com.kshiteesh.datingapp.identityservice.token.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kshiteesh.datingapp.identityservice.security.SecurityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshSessionServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");
	private static final UUID IDENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	private final RefreshSessionRepository repository = org.mockito.Mockito.mock(RefreshSessionRepository.class);
	private final RefreshTokenGenerator generator = org.mockito.Mockito.mock(RefreshTokenGenerator.class);
	private final RefreshTokenProtector protector = new RefreshTokenProtector("unit-test-refresh-token-secret");
	private final RefreshSessionService service = new RefreshSessionService(
			repository,
			generator,
			protector,
			properties(),
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void createSessionReturnsRawTokenButPersistsOnlyProtectedHash() {
		when(generator.generate()).thenReturn("raw-refresh-token");
		when(repository.saveAndFlush(any(RefreshSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RefreshToken refreshToken = service.createSession(IDENTITY_ID, "device-1", "Pixel");

		assertThat(refreshToken.value()).isEqualTo("raw-refresh-token");
		assertThat(refreshToken.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
		verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(session ->
				!session.getRefreshTokenHash().equals("raw-refresh-token")
						&& session.getRefreshTokenHash().matches("[0-9a-f]{64}")
						&& session.getIdentityId().equals(IDENTITY_ID)
						&& "device-1".equals(session.getDeviceId())
						&& "Pixel".equals(session.getDeviceName())));
	}

	@Test
	void successfulRefreshRotationRotatesOldSessionAndCreatesNewSession() {
		RefreshSession oldSession = activeSession("old-refresh-token", null, NOW.plus(Duration.ofDays(30)));
		when(repository.findByRefreshTokenHashForUpdate(protector.protect("old-refresh-token"))).thenReturn(Optional.of(oldSession));
		when(generator.generate()).thenReturn("new-refresh-token");
		when(repository.saveAndFlush(any(RefreshSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RefreshRotationResult result = service.rotate("old-refresh-token");

		assertThat(result.identityId()).isEqualTo(IDENTITY_ID);
		assertThat(result.refreshToken().value()).isEqualTo("new-refresh-token");
		assertThat(oldSession.getStatus()).isEqualTo(RefreshSessionStatus.ROTATED);
		assertThat(oldSession.getRotatedAt()).isEqualTo(NOW);
		verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(session ->
				protector.matches("new-refresh-token", session.getRefreshTokenHash())
						&& protector.protect("old-refresh-token").equals(session.getPreviousTokenHash())
						&& "device-1".equals(session.getDeviceId())
						&& "Pixel".equals(session.getDeviceName())));
	}

	@Test
	void repeatedRefreshRotationsPreserveDeviceMetadata() {
		RefreshSession firstSession = activeSession("first-refresh-token", null, NOW.plus(Duration.ofDays(30)));
		RefreshSession[] savedSessions = new RefreshSession[2];
		when(repository.findByRefreshTokenHashForUpdate(protector.protect("first-refresh-token")))
				.thenReturn(Optional.of(firstSession));
		when(generator.generate()).thenReturn("second-refresh-token");
		when(repository.saveAndFlush(any(RefreshSession.class))).thenAnswer(invocation -> {
			savedSessions[0] = invocation.getArgument(0);
			return savedSessions[0];
		});

		service.rotate("first-refresh-token");

		when(repository.findByRefreshTokenHashForUpdate(protector.protect("second-refresh-token")))
				.thenReturn(Optional.of(savedSessions[0]));
		when(generator.generate()).thenReturn("third-refresh-token");
		when(repository.saveAndFlush(any(RefreshSession.class))).thenAnswer(invocation -> {
			savedSessions[1] = invocation.getArgument(0);
			return savedSessions[1];
		});

		service.rotate("second-refresh-token");

		assertThat(savedSessions[0].getDeviceId()).isEqualTo("device-1");
		assertThat(savedSessions[0].getDeviceName()).isEqualTo("Pixel");
		assertThat(savedSessions[1].getDeviceId()).isEqualTo("device-1");
		assertThat(savedSessions[1].getDeviceName()).isEqualTo("Pixel");
	}

	@Test
	void oldTokenRejectedAfterRotationAndActiveChildRevokedOnReuse() {
		String oldHash = protector.protect("old-refresh-token");
		RefreshSession oldSession = activeSession("old-refresh-token", null, NOW.plus(Duration.ofDays(30)));
		oldSession.markRotated(NOW.minusSeconds(1));
		RefreshSession childSession = activeSession("new-refresh-token", oldHash, NOW.plus(Duration.ofDays(30)));
		when(repository.findByRefreshTokenHashForUpdate(oldHash)).thenReturn(Optional.of(oldSession));
		when(repository.findActiveSessionsByPreviousTokenHashForUpdate(oldHash)).thenReturn(List.of(childSession));

		assertThatThrownBy(() -> service.rotate("old-refresh-token"))
				.isInstanceOf(RefreshTokenException.class)
				.hasMessage("Refresh token cannot be used.");

		assertThat(oldSession.getStatus()).isEqualTo(RefreshSessionStatus.REUSE_DETECTED);
		assertThat(oldSession.getReuseDetectedAt()).isEqualTo(NOW);
		assertThat(childSession.getStatus()).isEqualTo(RefreshSessionStatus.REVOKED);
	}

	@Test
	void expiredRefreshTokenIsRejected() {
		RefreshSession expiredSession = activeSession("expired-token", null, NOW.minusSeconds(1));
		when(repository.findByRefreshTokenHashForUpdate(protector.protect("expired-token"))).thenReturn(Optional.of(expiredSession));

		assertThatThrownBy(() -> service.rotate("expired-token"))
				.isInstanceOf(RefreshTokenException.class)
				.hasMessage("Refresh token has expired.");

		assertThat(expiredSession.getStatus()).isEqualTo(RefreshSessionStatus.EXPIRED);
	}

	@Test
	void revokedRefreshTokenIsRejectedAndMarkedReuseDetected() {
		RefreshSession revokedSession = activeSession("revoked-token", null, NOW.plus(Duration.ofDays(30)));
		revokedSession.markRevoked(NOW.minusSeconds(1));
		when(repository.findByRefreshTokenHashForUpdate(protector.protect("revoked-token"))).thenReturn(Optional.of(revokedSession));
		when(repository.findActiveSessionsByPreviousTokenHashForUpdate(protector.protect("revoked-token"))).thenReturn(List.of());

		assertThatThrownBy(() -> service.rotate("revoked-token"))
				.isInstanceOf(RefreshTokenException.class)
				.hasMessage("Refresh token cannot be used.");

		assertThat(revokedSession.getStatus()).isEqualTo(RefreshSessionStatus.REUSE_DETECTED);
	}

	@Test
	void logoutRevokesActiveRefreshSession() {
		RefreshSession session = activeSession("refresh-token", null, NOW.plus(Duration.ofDays(30)));
		when(repository.findBySessionIdForUpdate(session.getSessionId())).thenReturn(Optional.of(session));

		service.logout(session.getSessionId());

		assertThat(session.getStatus()).isEqualTo(RefreshSessionStatus.REVOKED);
		assertThat(session.getRevokedAt()).isEqualTo(NOW);
	}

	@Test
	void logoutByRefreshTokenRevokesActiveRefreshSession() {
		RefreshSession session = activeSession("refresh-token", null, NOW.plus(Duration.ofDays(30)));
		when(repository.findByRefreshTokenHashForUpdate(protector.protect("refresh-token"))).thenReturn(Optional.of(session));

		service.logout("refresh-token");

		assertThat(session.getStatus()).isEqualTo(RefreshSessionStatus.REVOKED);
		assertThat(session.getRevokedAt()).isEqualTo(NOW);
	}

	private RefreshSession activeSession(String rawToken, String previousHash, Instant expiresAt) {
		return RefreshSession.active(
				IDENTITY_ID,
				protector.protect(rawToken),
				previousHash,
				NOW.minusSeconds(60),
				expiresAt,
				"device-1",
				"Pixel");
	}

	private static SecurityProperties properties() {
		return new SecurityProperties(
				"test-key",
				"private-key",
				"public-key",
				"identity-service",
				"dating-app",
				"otp-secret",
				"phone-secret",
				"refresh-token-secret",
				Duration.ofMinutes(15),
				Duration.ofDays(30));
	}
}
