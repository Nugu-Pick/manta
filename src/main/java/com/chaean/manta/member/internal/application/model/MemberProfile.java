package com.chaean.manta.member.internal.application.model;

import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberRole;

public record MemberProfile(long id, String nickname, String email, String gender, String ageGroup, String description,
                            Long avatarAssetId, MemberRole role) {

	public static MemberProfile from(Member member) {
		return new MemberProfile(member.getId(), member.getNickname(), member.getEmail(),
			member.getGender() == null ? null : member.getGender().name(),
			member.getAgeGroup() == null ? null : member.getAgeGroup().name(), member.getDescription(),
			member.getAvatarAssetId(), member.getRole());
	}
}
