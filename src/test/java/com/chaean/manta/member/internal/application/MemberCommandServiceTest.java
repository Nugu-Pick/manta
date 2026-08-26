package com.chaean.manta.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
