package com.chaean.manta.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaean.manta.member.fixture.MemberFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberOnboardingTest {

	@Test
	@DisplayName("새 회원은 온보딩 상태로 생성된다")
	void newMemberStartsOnboarding() {
		// given
		Member member = MemberFixture.create(42L, "user@example.com", "누구픽_abc12345");

		// then
		assertThat(ReflectionTestUtils.getField(member, "status")).isEqualTo(MemberStatus.ONBOARDING);
	}
}
