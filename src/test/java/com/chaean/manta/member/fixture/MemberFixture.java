package com.chaean.manta.member.fixture;

import com.chaean.manta.member.entity.AgeGroup;
import com.chaean.manta.member.entity.Gender;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberStatus;

import org.springframework.test.util.ReflectionTestUtils;

public final class MemberFixture {

	private MemberFixture() {
	}

	public static Member create(long id, String email, String nickname) {
		Member member = Member.register(email, nickname, null, null);
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	public static Member createActive(long id, String email, String nickname) {
		Member member = create(id, email, nickname);
		ReflectionTestUtils.setField(member, "status", MemberStatus.ACTIVE);
		return member;
	}

	public static Member createWithProfile(long id, String email, String nickname,
		String gender, String ageGroup, String description, Long avatarAssetId) {
		Member member = create(id, email, nickname);
		member.updateProfile(nickname, Gender.fromNullable(gender), AgeGroup.fromNullable(ageGroup), description,
			avatarAssetId);
		return member;
	}
}
