package com.kshiteesh.datingapp.identityservice.token.refresh;

import java.util.UUID;

public record RefreshRotationResult(
		UUID identityId,
		RefreshToken refreshToken
) {
}
