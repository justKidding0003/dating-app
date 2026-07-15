package com.kshiteesh.datingapp.identityservice.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kshiteesh.datingapp.identityservice.token.access.AccessToken;
import com.kshiteesh.datingapp.identityservice.token.access.AccessTokenService;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshRotationResult;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshSessionService;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshToken;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TokenOrchestrationServiceTest {

	private final AccessTokenService accessTokenService = org.mockito.Mockito.mock(AccessTokenService.class);
	private final RefreshSessionService refreshSessionService = org.mockito.Mockito.mock(RefreshSessionService.class);
	private final TokenOrchestrationService service = new TokenOrchestrationService(accessTokenService, refreshSessionService);

	@Test
	void issueAfterSuccessfulAuthenticationReturnsAccessTokenAndNewRefreshToken() {
		UUID identityId = UUID.randomUUID();
		AccessToken accessToken = new AccessToken("access-token", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(900));
		RefreshToken refreshToken = new RefreshToken("refresh-token", UUID.randomUUID(), Instant.now().plusSeconds(3600));
		when(accessTokenService.issue(identityId)).thenReturn(accessToken);
		when(refreshSessionService.createSession(identityId, "device-1", "Pixel")).thenReturn(refreshToken);

		TokenIssueResult result = service.issueAfterSuccessfulAuthentication(identityId, "device-1", "Pixel");

		assertThat(result.accessToken()).isSameAs(accessToken);
		assertThat(result.refreshToken()).isSameAs(refreshToken);
	}

	@Test
	void rotateReturnsNewAccessTokenAndNewRefreshToken() {
		UUID identityId = UUID.randomUUID();
		AccessToken accessToken = new AccessToken("new-access-token", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(900));
		RefreshToken refreshToken = new RefreshToken("new-refresh-token", UUID.randomUUID(), Instant.now().plusSeconds(3600));
		when(refreshSessionService.rotate("old-refresh-token")).thenReturn(new RefreshRotationResult(identityId, refreshToken));
		when(accessTokenService.issue(identityId)).thenReturn(accessToken);

		TokenIssueResult result = service.rotate("old-refresh-token");

		assertThat(result.accessToken()).isSameAs(accessToken);
		assertThat(result.refreshToken()).isSameAs(refreshToken);
	}
}
