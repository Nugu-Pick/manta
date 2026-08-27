package com.chaean.manta.member.internal.application;

import java.util.Optional;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.MemberAuthorization;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberRole;
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
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
        return MemberProfile.from(member);
    }

    @Transactional(readOnly = true)
    public PublicMemberProfile getPublicProfile(long memberId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
        return PublicMemberProfile.from(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberRole> findRoleBySubject(String subject) {
        return memberRepository.findBySupabaseSubjectAndDeletedAtIsNull(subject)
                .map(Member::getRole);
    }
}
