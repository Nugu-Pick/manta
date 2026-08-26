package com.chaean.manta.member.internal.application;

import java.text.Normalizer;
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
}
