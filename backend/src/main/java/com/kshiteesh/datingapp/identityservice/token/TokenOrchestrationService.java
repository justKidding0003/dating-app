package com.kshiteesh.datingapp.identityservice.token;

import com.kshiteesh.datingapp.identityservice.token.access.AccessToken;
import com.kshiteesh.datingapp.identityservice.token.access.AccessTokenService;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshRotationResult;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshSessionService;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshToken;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TokenOrchestrationService {

	private final AccessTokenService accessTokenService;
	private final RefreshSessionService refreshSessionService;

	public TokenOrchestrationService(AccessTokenService accessTokenService, RefreshSessionService refreshSessionService) {
		this.accessTokenService = accessTokenService;
		this.refreshSessionService = refreshSessionService;
	}

	public TokenIssueResult issueAfterSuccessfulAuthentication(UUID identityId, String deviceId, String deviceName) {
		AccessToken accessToken = accessTokenService.issue(identityId);
		RefreshToken refreshToken = refreshSessionService.createSession(identityId, deviceId, deviceName);
		return new TokenIssueResult(accessToken, refreshToken);
	}

	public TokenIssueResult rotate(String presentedRefreshToken) {
		RefreshRotationResult rotationResult = refreshSessionService.rotate(presentedRefreshToken);
		AccessToken accessToken = accessTokenService.issue(rotationResult.identityId());
		return new TokenIssueResult(accessToken, rotationResult.refreshToken());
	}
}
