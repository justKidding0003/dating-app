package com.kshiteesh.datingapp.identityservice.otp.repository;

import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallenge;
import com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

	Optional<OtpChallenge> findFirstByPhoneNumberHashAndStatusOrderByCreatedAtDesc(
			String phoneNumberHash,
			OtpChallengeStatus status);

	List<OtpChallenge> findByPhoneNumberHashAndStatusIn(String phoneNumberHash, Collection<OtpChallengeStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select challenge from OtpChallenge challenge where challenge.challengeId = :challengeId")
	Optional<OtpChallenge> findByChallengeIdForUpdate(@Param("challengeId") UUID challengeId);

	@Modifying
	@Query("""
			update OtpChallenge challenge
			set challenge.status = com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus.EXPIRED
			where challenge.phoneNumberHash = :phoneNumberHash
			and challenge.status = com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus.PENDING
			and challenge.challengeId <> :currentChallengeId
			""")
	int expireOtherPendingChallenges(
			@Param("phoneNumberHash") String phoneNumberHash,
			@Param("currentChallengeId") UUID currentChallengeId);

	@Modifying
	@Query("""
			update OtpChallenge challenge
			set challenge.status = com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus.EXPIRED
			where challenge.challengeId = :challengeId
			and challenge.status = com.kshiteesh.datingapp.identityservice.otp.entity.OtpChallengeStatus.PENDING
			""")
	int expirePendingChallenge(@Param("challengeId") UUID challengeId);

	long countByPhoneNumberHashAndCreatedAtAfter(String phoneNumberHash, Instant createdAfter);
}
