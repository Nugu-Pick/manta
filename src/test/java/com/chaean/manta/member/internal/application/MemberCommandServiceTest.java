package com.chaean.manta.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.api.event.MemberRegisteredEvent;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.internal.persistence.MemberRepository;

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
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    @Captor
    private ArgumentCaptor<MemberRegisteredEvent> eventCaptor;

    @Test
    @DisplayName("같은 Supabase subject가 이미 있으면 기존 회원을 재사용한다")
    void reusesExistingMemberForSameSubject() {
        // given
        Member member = Member.rehydrate(42L, "subject-1", "user@example.com", "누구픽_abc12345");
        when(memberRepository.findBySupabaseSubjectAndDeletedAtIsNull("subject-1")).thenReturn(Optional.of(member));
        MemberCommandService service = new MemberCommandService(memberRepository, eventPublisher);

        // when
        long memberId = service.ensureProvisioned(
                new AuthenticatedMember("subject-1", "user@example.com", "google"));

        // then
        assertThat(memberId).isEqualTo(42L);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("같은 이메일이어도 다른 Supabase subject면 새 회원으로 생성한다")
    void createsSeparateMemberForDifferentSubjectWithSameEmail() {
        // given
        when(memberRepository.findBySupabaseSubjectAndDeletedAtIsNull("subject-2")).thenReturn(Optional.empty());
        when(memberRepository.existsByNicknameAndDeletedAtIsNull(any())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(
                Member.rehydrate(43L, "subject-2", "user@example.com", "누구픽_abc12345"));
        MemberCommandService service = new MemberCommandService(memberRepository, eventPublisher);

        // when
        service.ensureProvisioned(new AuthenticatedMember("subject-2", "user@example.com", "kakao"));

        // then
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getSupabaseSubject()).isEqualTo("subject-2");
        assertThat(memberCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(memberCaptor.getValue().getNickname()).matches("누구픽_[0-9a-f]{8}");
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().memberId()).isEqualTo(43L);
    }

    @Test
    @DisplayName("이메일이 없으면 공통 ErrorCode를 사용한다")
    void usesCommonErrorCodeForMissingEmail() {
        // given
        MemberCommandService service = new MemberCommandService(memberRepository, eventPublisher);

        // when & then
        assertThatThrownBy(() -> service.ensureProvisioned(
                new AuthenticatedMember("subject-1", "", "google")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isSameAs(ErrorCode.EMAIL_REQUIRED));
    }

    @Test
    @DisplayName("회원 프로필을 수정하고 닉네임을 정규화한다")
    void updatesProfileWithNormalizedNickname() {
        // given
        Member member = Member.rehydrate(42L, "subject-1", "user@example.com", "기존닉네임");
        when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndDeletedAtIsNull("새 닉네임")).thenReturn(false);
        MemberCommandService service = new MemberCommandService(memberRepository, eventPublisher);

        // when
        service.updateProfile(42L, new MemberProfileUpdate("  새 닉네임  ", "FEMALE", "TWENTIES",
                "소개입니다.", 99L));

        // then
        assertThat(member.getNickname()).isEqualTo("새 닉네임");
        assertThat(member.getGender()).isEqualTo("FEMALE");
        assertThat(member.getAgeGroup()).isEqualTo("TWENTIES");
        assertThat(member.getBio()).isEqualTo("소개입니다.");
        assertThat(member.getAvatarAssetId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("사용 중인 닉네임으로 변경하면 충돌 오류를 반환한다")
    void rejectsDuplicateNickname() {
        // given
        Member member = Member.rehydrate(42L, "subject-1", "user@example.com", "기존닉네임");
        when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndDeletedAtIsNull("다른회원")).thenReturn(true);
        MemberCommandService service = new MemberCommandService(memberRepository, eventPublisher);

        // when & then
        assertThatThrownBy(() -> service.updateProfile(42L,
                new MemberProfileUpdate("다른회원", null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isSameAs(ErrorCode.NICKNAME_ALREADY_TAKEN));
    }

    @Test
    @DisplayName("공백만 있는 닉네임은 변경할 수 없다")
    void rejectsBlankNickname() {
        // given
        Member member = Member.rehydrate(42L, "subject-1", "user@example.com", "기존닉네임");
        when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
        MemberCommandService service = new MemberCommandService(memberRepository, eventPublisher);

        // when & then
        assertThatThrownBy(() -> service.updateProfile(42L,
                new MemberProfileUpdate("   ", null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isSameAs(ErrorCode.NICKNAME_INVALID));
    }

    @Test
    @DisplayName("회원 탈퇴 시 개인정보를 익명화하고 삭제 시각을 기록한다")
    void anonymizesMemberOnWithdrawal() {
        // given
        Member member = Member.rehydrate(42L, "subject-1", "user@example.com", "누구픽_abc12345",
                "MALE", "THIRTIES", "소개", 99L);
        when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
        MemberCommandService service = new MemberCommandService(memberRepository, eventPublisher);

        // when
        service.withdraw(42L, Instant.parse("2026-08-26T00:00:00Z"));

        // then
        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDeletedAt()).isEqualTo(Instant.parse("2026-08-26T00:00:00Z"));
        assertThat(member.getSupabaseSubject()).isNull();
        assertThat(member.getEmail()).isNull();
        assertThat(member.getGender()).isNull();
        assertThat(member.getAgeGroup()).isNull();
        assertThat(member.getBio()).isNull();
        assertThat(member.getAvatarAssetId()).isNull();
    }
}
