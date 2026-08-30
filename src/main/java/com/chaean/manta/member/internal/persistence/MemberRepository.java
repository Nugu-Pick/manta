package com.chaean.manta.member.internal.persistence;

import java.util.Optional;

import com.chaean.manta.member.entity.Member;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByNicknameAndDeletedAtIsNull(String nickname);

	Optional<Member> findByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT member FROM Member member WHERE member.id = :memberId AND member.deletedAt IS NULL")
	Optional<Member> findByIdAndDeletedAtIsNullForUpdate(@Param("memberId") Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT member FROM Member member "
		+ "WHERE LOWER(member.email) = LOWER(:email) AND member.deletedAt IS NULL ORDER BY member.id")
	Optional<Member> findFirstByEmailAndDeletedAtIsNullOrderByIdAsc(@Param("email") String email);

	@Query(value = "WITH advisory_lock AS ("
		+ "SELECT pg_advisory_xact_lock(hashtextextended(CAST(:email AS text), 0))) "
		+ "SELECT 1 FROM advisory_lock", nativeQuery = true)
	Integer lockEmail(@Param("email") String email);
}
