package com.chaean.manta.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Optional;

import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberIdentity;
import com.chaean.manta.member.fixture.MemberFixture;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.OAuthLoginResult;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;
import com.chaean.manta.member.internal.persistence.MemberRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthCommandServiceTest {

	@Test
	@DisplayName("기존 identity OAuth 로그인은 회원과 identity의 로그인 시각을 갱신한다")
	void updatesLoginTimestampsForExistingIdentity() {
		// given
		Instant now = Instant.parse("2026-08-31T00:00:00Z");
		Member member = MemberFixture.createActive(42L, "user@example.com", "누구픽_abc12345");
		MemberIdentity identity = MemberIdentity.create(42L, "google", "subject", now.minusSeconds(60));
		MemberIdentityRepository identityRepository = mock(MemberIdentityRepository.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		AuthTokenCommandService tokenService = mock(AuthTokenCommandService.class);
		OAuthProfile profile = new OAuthProfile(OAuthProvider.GOOGLE, "subject", "user@example.com");
		AuthTokenPair tokens = mock(AuthTokenPair.class);
		when(identityRepository.findByProviderAndProviderSubject("google", "subject"))
			.thenReturn(Optional.of(identity));
		when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
		when(tokenService.issueAccessAndRefreshTokenPair(42L, now)).thenReturn(tokens);
		OAuthCommandService service = new OAuthCommandService(identityRepository, memberRepository, tokenService);

		// when
		OAuthLoginResult result = service.completeOAuthLogin(profile, now);

		// then
		assertThat(result.tokens()).isEqualTo(tokens);
		assertThat(member.getLastLoginProvider()).isEqualTo("google");
		assertThat(member.getLastLoginAt()).isEqualTo(now);
		assertThat(identity.getLastLoginAt()).isEqualTo(now);
		verify(tokenService).issueAccessAndRefreshTokenPair(42L, now);
	}

	@Test
	@DisplayName("신규 OAuth profile은 회원과 token 없이 signup context만 반환한다")
	void returnsSignupContextForNewProfile() {
		// given
		Instant now = Instant.parse("2026-08-31T00:00:00Z");
		MemberIdentityRepository identityRepository = mock(MemberIdentityRepository.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		AuthTokenCommandService tokenService = mock(AuthTokenCommandService.class);
		OAuthProfile profile = new OAuthProfile(OAuthProvider.NAVER, "subject", "User@Example.com");
		when(identityRepository.findByProviderAndProviderSubject("naver", "subject"))
			.thenReturn(Optional.empty());
		OAuthCommandService service = new OAuthCommandService(identityRepository, memberRepository, tokenService);

		// when
		OAuthLoginResult result = service.completeOAuthLogin(profile, now);

		// then
		assertThat(result.requiresSignup()).isTrue();
		assertThat(result.signupContext().provider()).isEqualTo(OAuthProvider.NAVER);
		assertThat(result.signupContext().email()).isEqualTo("user@example.com");
		assertThat(result.signupContext().expiresAt()).isEqualTo(now.plusSeconds(600));
		verifyNoInteractions(memberRepository, tokenService);
	}
}
