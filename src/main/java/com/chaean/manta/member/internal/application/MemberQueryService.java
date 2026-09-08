package com.chaean.manta.member.internal.application;

import java.util.Optional;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.MemberAuthorization;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberRole;
import com.chaean.manta.member.entity.MemberStatus;
import com.chaean.manta.member.internal.application.model.MemberProfile;
import com.chaean.manta.member.internal.application.model.PublicMemberProfile;
import com.chaean.manta.member.internal.persistence.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberAuthorization {

	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public MemberProfile getMyProfile(long memberId) {
		Member member = findActiveMember(memberId);

		return MemberProfile.from(member);
	}

	@Transactional(readOnly = true)
	public PublicMemberProfile getPublicProfile(long memberId) {
		Member member = findActiveMember(memberId);

		return PublicMemberProfile.from(member);
	}

	private Member findActiveMember(long memberId) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));

		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw BusinessException.of(ErrorCode.MEMBER_STATUS_NOT_ACTIVE);
		}

		return member;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MemberRole> findRoleByMemberId(long memberId) {
		return memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.filter(member -> member.getStatus() == MemberStatus.ACTIVE)
			.map(Member::getRole);
	}
}
