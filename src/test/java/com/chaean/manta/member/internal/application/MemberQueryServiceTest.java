package com.chaean.manta.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberRole;
import com.chaean.manta.member.internal.application.MemberProfile;
import com.chaean.manta.member.internal.persistence.MemberRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 조회 Service는 HTTP 응답 DTO가 아닌 애플리케이션 조회 모델을 반환한다")
    void returnsApplicationReadModel() {
        // given
        Member member = Member.rehydrate(42L, "subject-1", "user@example.com", "누구픽_abc12345");
        when(memberRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));
        MemberQueryService service = new MemberQueryService(memberRepository);

        // when
        MemberProfile result = service.getMyProfile(42L);

        // then
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.nickname()).isEqualTo("누구픽_abc12345");
    }

    @Test
    @DisplayName("회원 subject로 회원 역할을 조회한다")
    void findsRoleBySubject() {
        // given
        Member member = Member.rehydrate(42L, "subject-1", "user@example.com", "누구픽_abc12345");
        when(memberRepository.findBySupabaseSubjectAndDeletedAtIsNull("subject-1"))
                .thenReturn(Optional.of(member));
        MemberQueryService service = new MemberQueryService(memberRepository);

        // when
        Optional<MemberRole> result = service.findRoleBySubject("subject-1");

        // then
        assertThat(result).contains(MemberRole.USER);
    }
}
