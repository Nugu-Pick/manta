package com.chaean.manta.member.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.chaean.manta.member.entity.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	@Query("SELECT token.familyId FROM RefreshToken token WHERE token.tokenHash = :tokenHash")
	Optional<UUID> findFamilyIdByTokenHash(@Param("tokenHash") String tokenHash);

	@Query("SELECT token FROM RefreshToken token WHERE token.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT token FROM RefreshToken token WHERE token.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshToken> findFirstByFamilyIdOrderByIdAsc(UUID familyId);

	List<RefreshToken> findAllByFamilyId(UUID familyId);

	List<RefreshToken> findAllByMemberId(Long memberId);
}
