package com.chaean.manta.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.event.MemberRegisteredEvent;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.fixture.MemberFixture;
import com.chaean.manta.member.internal.application.model.MemberProfileUpdate;
import com.chaean.manta.member.internal.persistence.MemberRepository;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;
import com.chaean.manta.member.internal.persistence.RefreshTokenRepository;
import com.chaean.manta.member.internal.persistence.LegalDocumentRepository;
import com.chaean.manta.member.internal.persistence.MemberAgreementRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MemberIdentityRepository memberIdentityRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private LegalDocumentRepository legalDocumentRepository;

	@Mock
	private MemberAgreementRepository memberAgreementRepository;

	@Mock
	private LegalDocumentQueryService legalDocumentQueryService;

	@Mock
	private org.springframework.context.ApplicationEventPublisher eventPublisher;

	@Captor
	private ArgumentCaptor<Member> memberCaptor;

	@Captor
	private ArgumentCaptor<MemberRegisteredEvent> eventCaptor;

	@Test
	@DisplayName("이메일로 새 회원을 온보딩 상태로 생성한다")
	void createsNewMemberInOnboardingStatus() {
		// given
		when(memberRepository.existsByNicknameAndDeletedAtIsNull(any())).thenReturn(false);
		when(memberRepository.save(any(Member.class))).thenReturn(
			MemberFixture.create(43L, "user@example.com", "누구픽_abc12345"));
		MemberCommandService service = service();

		// when
		service.register("user@example.com");

		// then
		verify(memberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getEmail()).isEqualTo("user@example.com");
		assertThat(memberCaptor.getValue().getStatus()).isEqualTo(com.chaean.manta.member.entity.MemberStatus.ONBOARDING);
		assertThat(memberCaptor.getValue().getNickname()).matches("누구픽_[0-9a-f]{8}");
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().memberId()).isEqualTo(43L);
	}

	@Test
	@DisplayName("이메일이 없으면 공통 ErrorCode를 사용한다")
	void usesCommonErrorCodeForMissingEmail() {
		// given
		MemberCommandService service = service();

		// when & then
		assertThatThrownBy(() -> service.register(""))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.errorCode()).isSameAs(ErrorCode.EMAIL_REQUIRED));
	}

	@Test
	@DisplayName("회원 프로필을 수정하고 닉네임을 정규화한다")
	void updatesProfileWithNormalizedNickname() {
		// given
		Member member = MemberFixture.createActive(42L, "user@example.com", "기존닉네임");
		when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedAtIsNull("새 닉네임")).thenReturn(false);
		MemberCommandService service = service();

		// when
		service.updateProfile(42L, MemberProfileUpdate.of("  새 닉네임  ", "FEMALE", "TWENTIES",
			"소개입니다.", 99L));

		// then
		assertThat(member.getNickname()).isEqualTo("새 닉네임");
		assertThat(member.getGender()).isEqualTo("FEMALE");
		assertThat(member.getAgeGroup()).isEqualTo("TWENTIES");
		assertThat(member.getDescription()).isEqualTo("소개입니다.");
		assertThat(member.getAvatarAssetId()).isEqualTo(99L);
	}

	@Test
	@DisplayName("사용 중인 닉네임으로 변경하면 충돌 오류를 반환한다")
	void rejectsDuplicateNickname() {
		// given
		Member member = MemberFixture.createActive(42L, "user@example.com", "기존닉네임");
		when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedAtIsNull("다른회원")).thenReturn(true);
		MemberCommandService service = service();

		// when & then
		assertThatThrownBy(() -> service.updateProfile(42L,
			MemberProfileUpdate.of("다른회원", null, null, null, null)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.errorCode()).isSameAs(ErrorCode.NICKNAME_ALREADY_TAKEN));
	}

	@Test
	@DisplayName("공백만 있는 닉네임은 변경할 수 없다")
	void rejectsBlankNickname() {
		// given
		Member member = MemberFixture.createActive(42L, "user@example.com", "기존닉네임");
		when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
		MemberCommandService service = service();

		// when & then
		assertThatThrownBy(() -> service.updateProfile(42L,
			MemberProfileUpdate.of("   ", null, null, null, null)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.errorCode()).isSameAs(ErrorCode.NICKNAME_INVALID));
	}

	private MemberCommandService service() {
		return new MemberCommandService(memberRepository, memberIdentityRepository, refreshTokenRepository,
			legalDocumentRepository, memberAgreementRepository, legalDocumentQueryService, eventPublisher);
	}

}
