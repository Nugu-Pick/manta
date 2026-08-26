package com.chaean.manta.member.internal.persistence;

import java.util.Optional;

import com.chaean.manta.member.entity.Member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findBySupabaseSubjectAndDeletedAtIsNull(String supabaseSubject);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);
}
