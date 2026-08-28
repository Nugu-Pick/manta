package com.chaean.manta.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaean.manta.member.fixture.MemberFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

	@Test
	@DisplayName("프로필 수정 시 null인 값은 기존 값을 유지한다")
	void keepsExistingProfileValuesWhenUpdateValueIsNull() {
		// given
		Member member = MemberFixture.createWithProfile(42L, "subject-1", "user@example.com", "기존닉네임",
			"FEMALE", "TWENTIES", "기존 설명", 99L);

		// when
		member.updateProfile("새 닉네임", null, null, null, null);

		// then
		assertThat(member.getNickname()).isEqualTo("새 닉네임");
		assertThat(member.getGender()).isEqualTo("FEMALE");
		assertThat(member.getAgeGroup()).isEqualTo("TWENTIES");
		assertThat(member.getDescription()).isEqualTo("기존 설명");
		assertThat(member.getAvatarAssetId()).isEqualTo(99L);
	}
}
