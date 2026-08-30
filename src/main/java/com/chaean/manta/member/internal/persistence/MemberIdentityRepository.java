package com.chaean.manta.member.internal.persistence;

import java.util.Optional;

import com.chaean.manta.member.entity.MemberIdentity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberIdentityRepository extends JpaRepository<MemberIdentity, Long> {

	Optional<MemberIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);

	void deleteAllByMemberId(Long memberId);
}
