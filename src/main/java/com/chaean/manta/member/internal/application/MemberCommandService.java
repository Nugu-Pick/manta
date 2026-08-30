package com.chaean.manta.member.internal.application;

import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.event.MemberRegisteredEvent;
import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberIdentity;
import com.chaean.manta.member.entity.MemberStatus;
import com.chaean.manta.member.internal.application.model.MemberOnboarding;
import com.chaean.manta.member.internal.application.model.MemberOnboardingResult;
import com.chaean.manta.member.internal.application.model.MemberProfileUpdate;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.persistence.LegalDocumentRepository;
import com.chaean.manta.member.internal.persistence.MemberAgreementRepository;
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
	private final LegalDocumentRepository legalDocumentRepository;
	private final MemberAgreementRepository memberAgreementRepository;
	private final LegalDocumentQueryService legalDocumentQueryService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public Member register(String email) {
		if (email == null || email.isBlank()) {
			throw BusinessException.of(ErrorCode.EMAIL_REQUIRED);
		}

		for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
			String nickname = normalize(createNickname());

			if (memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
				continue;
			}

			Member member = memberRepository.save(Member.register(email, nickname));
			eventPublisher.publishEvent(MemberRegisteredEvent.of(member.getId()));
			return member;
		}

		throw BusinessException.of(ErrorCode.NICKNAME_GENERATION_FAILED);
	}

	@Transactional
	public Member findOrRegisterOAuthMember(OAuthProvider provider, OAuthProfile profile, Instant now) {
		String email = normalizeEmail(profile.email());

		// 신규 이메일에는 잠글 회원 row가 없으므로 PostgreSQL advisory lock으로 생성 경합을
		// 직렬화한다.
		memberRepository.lockEmail(email);

		MemberIdentity identity = memberIdentityRepository
			.findByProviderAndProviderSubject(provider.value(), profile.providerSubject())
			.orElse(null);

		Member member;
		if (identity != null) {
			member = memberRepository.findByIdAndDeletedAtIsNullForUpdate(identity.getMemberId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
			identity.recordLogin(now);
		} else {
			member = memberRepository.findFirstByEmailAndDeletedAtIsNullOrderByIdAsc(email)
				.orElseGet(() -> register(email));
			memberIdentityRepository.save(
				MemberIdentity.create(member.getId(), provider.value(), profile.providerSubject(), now));
		}

		member.recordLogin(provider.value(), now);

		return member;
	}

	@Transactional
	public MemberOnboardingResult completeOnboarding(long memberId, MemberOnboarding onboarding) {
		Member member = memberRepository.findByIdAndDeletedAtIsNullForUpdate(memberId)
			.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));

		if (member.getStatus() != MemberStatus.ONBOARDING) {
			throw BusinessException.of(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
		}

		Set<Long> requestedIds = new LinkedHashSet<>(onboarding.legalDocumentIds());
		List<LegalDocument> requestedDocuments = legalDocumentRepository.findAllById(requestedIds);

		if (requestedDocuments.size() != requestedIds.size()) {
			throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND);
		}

		List<LegalDocument> currentDocuments = legalDocumentQueryService.findCurrentDocuments();
		Set<Long> currentIds = currentDocuments.stream()
			.map(LegalDocument::getId)
			.collect(java.util.stream.Collectors.toSet());
		Set<Long> requiredIds = currentDocuments.stream()
			.filter(LegalDocument::isRequired)
			.map(LegalDocument::getId)
			.collect(java.util.stream.Collectors.toSet());

		if (!requestedIds.containsAll(requiredIds)) {
			throw BusinessException.of(ErrorCode.ONBOARDING_REQUIRED_AGREEMENT_MISSING);
		}

		if (!currentIds.containsAll(requestedIds)) {
			throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_AVAILABLE);
		}

		for (Long documentId : requestedIds) {
			memberAgreementRepository.createIfAbsent(memberId, documentId);
		}

		member.completeOnboarding(onboarding.gender(), onboarding.ageGroup());

		return MemberOnboardingResult.of(member.getId(), member.getStatus());
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

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw BusinessException.of(ErrorCode.AUTH_PROFILE_INVALID);
		}

		return email.trim().toLowerCase(Locale.ROOT);
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
