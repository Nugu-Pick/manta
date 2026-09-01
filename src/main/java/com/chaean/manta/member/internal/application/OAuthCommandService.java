package com.chaean.manta.member.internal.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberIdentity;
import com.chaean.manta.member.internal.application.model.OAuthLoginResult;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.SignupContext;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;
import com.chaean.manta.member.internal.persistence.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthCommandService {

	private static final Duration SIGNUP_CONTEXT_TTL = Duration.ofMinutes(10);

	private final MemberIdentityRepository memberIdentityRepository;
	private final MemberRepository memberRepository;
	private final AuthTokenCommandService authTokenCommandService;

	@Transactional
	public OAuthLoginResult completeOAuthLogin(OAuthProfile profile, Instant now) {
		String provider = profile.provider().value();
		return memberIdentityRepository.findByProviderAndProviderSubject(provider, profile.providerSubject())
			.map(identity -> existingMember(identity, profile, now))
			.orElseGet(() -> OAuthLoginResult.signup(new SignupContext(profile.provider(), profile.providerSubject(),
				profile.email().trim().toLowerCase(Locale.ROOT), now, now.plus(SIGNUP_CONTEXT_TTL))));
	}

	private OAuthLoginResult existingMember(MemberIdentity identity, OAuthProfile profile, Instant now) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(identity.getMemberId())
			.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
		member.recordLogin(profile.provider().value(), now);
		identity.recordLogin(now);
		return OAuthLoginResult.login(authTokenCommandService.issueAccessAndRefreshTokenPair(member.getId(), now));
	}
}
