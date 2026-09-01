package com.chaean.manta.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberIdentity;
import com.chaean.manta.member.fixture.MemberFixture;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.application.model.SignupCommand;
import com.chaean.manta.member.internal.application.model.SignupContext;
import com.chaean.manta.member.internal.persistence.LegalDocumentRepository;
import com.chaean.manta.member.internal.persistence.MemberAgreementRepository;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignupCommandServiceTest {

	@Mock
	private SignupContextCodec contextCodec;

	@Mock
	private MemberCommandService memberCommandService;

	@Mock
	private MemberIdentityRepository memberIdentityRepository;

	@Mock
	private MemberAgreementRepository memberAgreementRepository;

	@Mock
	private LegalDocumentRepository legalDocumentRepository;

	@Mock
	private LegalDocumentQueryService legalDocumentQueryService;

	@Mock
	private AuthTokenCommandService authTokenCommandService;

	@Test
	@DisplayName("필수 약관 동의와 선택 프로필을 검증해 가입을 원자적으로 실행한다")
	void completesSignup() {
		// given
		Instant now = Instant.parse("2026-08-31T00:00:00Z");
		SignupContext context = new SignupContext(OAuthProvider.GOOGLE, "subject", "user@example.com", now,
			now.plusSeconds(600));
		SignupCommand command = SignupCommand.of(List.of(10L), null, null);
		LegalDocument requiredDocument = org.mockito.Mockito.mock(LegalDocument.class);
		when(requiredDocument.getId()).thenReturn(10L);
		when(requiredDocument.isRequired()).thenReturn(true);
		Member member = MemberFixture.createActive(42L, "user@example.com", "누구픽_abc12345");
		AuthTokenPair tokens = AuthTokenPair.of("access", now.plusSeconds(1800), "refresh", now.plusSeconds(3600),
			com.chaean.manta.member.entity.MemberStatus.ACTIVE);
		when(contextCodec.decode("context", now)).thenReturn(context);
		when(legalDocumentRepository.findAllById(List.of(10L))).thenReturn(List.of(requiredDocument));
		when(legalDocumentQueryService.findCurrentDocuments()).thenReturn(List.of(requiredDocument));
		when(memberCommandService.registerSignupMember("user@example.com", null, null)).thenReturn(member);
		when(authTokenCommandService.issueAccessAndRefreshTokenPair(42L, now)).thenReturn(tokens);
		SignupCommandService service = service();

		// when
		AuthTokenPair result = service.complete("context", command, now);

		// then
		assertThat(result).isEqualTo(tokens);
		verify(memberIdentityRepository).save(any(MemberIdentity.class));
		verify(memberAgreementRepository).createIfAbsent(42L, 10L);
	}

	@Test
	@DisplayName("필수 약관이 빠지면 회원을 생성하지 않는다")
	void rejectsMissingRequiredDocument() {
		// given
		Instant now = Instant.parse("2026-08-31T00:00:00Z");
		SignupContext context = new SignupContext(OAuthProvider.KAKAO, "subject", "user@example.com", now,
			now.plusSeconds(600));
		LegalDocument requiredDocument = org.mockito.Mockito.mock(LegalDocument.class);
		when(requiredDocument.getId()).thenReturn(10L);
		when(requiredDocument.isRequired()).thenReturn(true);
		when(contextCodec.decode("context", now)).thenReturn(context);
		when(legalDocumentRepository.findAllById(List.of())).thenReturn(List.of());
		when(legalDocumentQueryService.findCurrentDocuments()).thenReturn(List.of(requiredDocument));
		SignupCommandService service = service();

		// when & then
		assertThatThrownBy(() -> service.complete("context", SignupCommand.of(List.of(), "MALE", "TEENS"), now))
			.isInstanceOf(BusinessException.class);
		org.mockito.Mockito.verifyNoInteractions(memberCommandService, memberAgreementRepository, authTokenCommandService);
	}

	private SignupCommandService service() {
		return new SignupCommandService(contextCodec, memberCommandService, memberIdentityRepository,
			memberAgreementRepository, legalDocumentRepository, legalDocumentQueryService, authTokenCommandService);
	}
}
