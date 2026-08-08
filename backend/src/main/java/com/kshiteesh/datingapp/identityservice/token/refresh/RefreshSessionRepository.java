package com.kshiteesh.datingapp.identityservice.token.refresh;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from RefreshSession session where session.refreshTokenHash = :refreshTokenHash")
	Optional<RefreshSession> findByRefreshTokenHashForUpdate(@Param("refreshTokenHash") String refreshTokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from RefreshSession session where session.sessionId = :sessionId")
	Optional<RefreshSession> findBySessionIdForUpdate(@Param("sessionId") UUID sessionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select session from RefreshSession session
			where session.previousTokenHash = :previousTokenHash
			and session.status = com.kshiteesh.datingapp.identityservice.token.refresh.RefreshSessionStatus.ACTIVE
			""")
	List<RefreshSession> findActiveSessionsByPreviousTokenHashForUpdate(@Param("previousTokenHash") String previousTokenHash);
}
