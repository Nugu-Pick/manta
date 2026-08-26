package com.chaean.manta.member.internal.application;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.event.MemberRegisteredEvent;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.internal.persistence.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private static final int MAX_NICKNAME_ATTEMPTS = 5;

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public long ensureProvisioned(AuthenticatedMember authenticatedMember) {
        if (authenticatedMember.email().isBlank()) {
            throw BusinessException.of(ErrorCode.EMAIL_REQUIRED);
        }

        return memberRepository.findBySupabaseSubjectAndDeletedAtIsNull(authenticatedMember.subject())
                .map(Member::getId)
                .orElseGet(() -> register(authenticatedMember));
    }

    @Transactional
    public void updateProfile(long memberId, MemberProfileUpdate update) {
        Member member = findActiveMember(memberId);
        String nickname = update.nickname() == null ? member.getNickname() : normalizeNickname(update.nickname());
        if (nickname.isBlank() || nickname.length() > 32) {
            throw BusinessException.of(ErrorCode.NICKNAME_INVALID);
        }
        if (!nickname.equals(member.getNickname()) && memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw BusinessException.of(ErrorCode.NICKNAME_ALREADY_TAKEN);
        }
        member.updateProfile(nickname, updateValue(update.gender(), member.getGender()),
                updateValue(update.ageGroup(), member.getAgeGroup()), updateValue(update.bio(), member.getBio()),
                update.avatarAssetId() == null ? member.getAvatarAssetId() : update.avatarAssetId());
    }

    @Transactional
    public void withdraw(long memberId, Instant deletedAt) {
        findActiveMember(memberId).withdraw(deletedAt);
    }

    private long register(AuthenticatedMember authenticatedMember) {
        for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
            String nickname = normalize(createNickname());
            if (memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
                continue;
            }
            Member member = memberRepository.save(Member.register(authenticatedMember.subject(), authenticatedMember.email(), nickname));

            eventPublisher.publishEvent(MemberRegisteredEvent.of(member.getId()));

            return member.getId();
        }
        throw BusinessException.of(ErrorCode.NICKNAME_GENERATION_FAILED);
    }

    private String createNickname() {
        return "누구픽_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String normalize(String nickname) {
        return Normalizer.normalize(nickname, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNickname(String nickname) {
        return normalize(nickname);
    }

    private String updateValue(String value, String currentValue) {
        return value == null ? currentValue : value.trim().isEmpty() ? null : value.trim();
    }

    private Member findActiveMember(long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
    }
}
