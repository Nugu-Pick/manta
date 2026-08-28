package com.chaean.manta.member.internal.application;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.event.MemberRegisteredEvent;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.internal.application.model.MemberProfileUpdate;
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
		String nickname = update.nickname() == null
			? member.getNickname()
			: normalize(update.nickname());
		if (nickname.isBlank() || nickname.length() > 32) {
			throw BusinessException.of(ErrorCode.NICKNAME_INVALID);
		}
		if (!nickname.equals(member.getNickname())
			&& memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
			throw BusinessException.of(ErrorCode.NICKNAME_ALREADY_TAKEN);
		}
		member.updateProfile(nickname, update.gender(), update.ageGroup(), update.description(),
			update.avatarAssetId());
	}

	@Transactional
	public void deleteMyAccount(long memberId) {
		Member member = findActiveMember(memberId);
		member.clearLoginIdentity();
		memberRepository.flush();
		memberRepository.delete(member);
	}

	private long register(AuthenticatedMember authenticatedMember) {
		for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
			String nickname = normalize(createNickname());
			if (memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
				continue;
			}
			Member member = memberRepository.save(
				Member.register(authenticatedMember.subject(), authenticatedMember.email(), nickname));

			eventPublisher.publishEvent(MemberRegisteredEvent.of(member.getId()));

			return member.getId();
		}
		throw BusinessException.of(ErrorCode.NICKNAME_GENERATION_FAILED);
	}

	private String createNickname() {
		return "누구픽_" + UUID.randomUUID().toString()
			.replace("-", "")
			.substring(0, 8);
	}

	private String normalize(String nickname) {
		return Normalizer.normalize(nickname, Normalizer.Form.NFKC)
			.trim()
			.toLowerCase(Locale.ROOT);
	}

	private Member findActiveMember(long memberId) {
		return memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
	}
}
