package com.chaean.manta.member.internal.application;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.event.MemberRegisteredEvent;
import com.chaean.manta.member.entity.AgeGroup;
import com.chaean.manta.member.entity.Gender;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberStatus;
import com.chaean.manta.member.internal.application.model.MemberProfileUpdate;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;
import com.chaean.manta.member.internal.persistence.MemberRepository;
import com.chaean.manta.member.internal.persistence.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

	private static final int MAX_NICKNAME_ATTEMPTS = 5;

	private final MemberRepository memberRepository;
	private final MemberIdentityRepository memberIdentityRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public Member registerSignupMember(String email, Gender gender, AgeGroup ageGroup) {
		if (email == null || email.isBlank()) {
			throw BusinessException.of(ErrorCode.EMAIL_REQUIRED);
		}

		for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
			String nickname = normalize(createNickname());

			if (memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
				continue;
			}

			Member member = memberRepository.save(Member.register(email, nickname, gender, ageGroup));
			eventPublisher.publishEvent(MemberRegisteredEvent.of(member.getId()));
			return member;
		}

		throw BusinessException.of(ErrorCode.NICKNAME_GENERATION_FAILED);
	}

	@Transactional
	public void updateProfile(long memberId, MemberProfileUpdate update) {
		Member member = findActiveMember(memberId);
		validateProfileValues(update);

		String nickname = update.nickname() == null
			? member.getNickname()
			: normalize(update.nickname());

		if (nickname.isBlank() || nickname.length() > 32) {
			throw BusinessException.of(ErrorCode.NICKNAME_INVALID);
		}

		if (!nickname.equals(member.getNickname()) && memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
			throw BusinessException.of(ErrorCode.NICKNAME_ALREADY_TAKEN);
		}

		member.updateProfile(nickname, Gender.fromNullable(update.gender()), AgeGroup.fromNullable(update.ageGroup()),
			update.description(), update.avatarAssetId());
	}

	private void validateProfileValues(MemberProfileUpdate update) {
		if (!Gender.isValidOrNull(update.gender())) {
			throw BusinessException.of(ErrorCode.SIGNUP_GENDER_INVALID);
		}
		if (!AgeGroup.isValidOrNull(update.ageGroup())) {
			throw BusinessException.of(ErrorCode.SIGNUP_AGE_GROUP_INVALID);
		}
	}

	@Transactional
	public void deleteMyAccount(long memberId) {
		Member member = memberRepository.findByIdAndDeletedAtIsNullForUpdate(memberId)
			.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw BusinessException.of(ErrorCode.MEMBER_STATUS_NOT_ACTIVE);
		}

		member.withdraw();
		Instant revokedAt = Instant.now();

		refreshTokenRepository.findAllByMemberId(memberId).forEach(token -> token.revoke(revokedAt));
		memberIdentityRepository.deleteAllByMemberId(memberId);

		memberRepository.flush();
		memberRepository.delete(member);
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
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));

		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw BusinessException.of(ErrorCode.MEMBER_STATUS_NOT_ACTIVE);
		}

		return member;
	}
}
