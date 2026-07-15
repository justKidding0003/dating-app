package com.kshiteesh.datingapp.identityservice.token;

import com.kshiteesh.datingapp.identityservice.token.access.AccessToken;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshToken;

public record TokenIssueResult(
		AccessToken accessToken,
		RefreshToken refreshToken
) {
}
