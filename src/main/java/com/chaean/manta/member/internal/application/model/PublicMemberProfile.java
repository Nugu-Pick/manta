package com.chaean.manta.member.internal.application.model;

import com.chaean.manta.member.entity.Member;

public record PublicMemberProfile(long id, String nickname, String gender, String ageGroup, String description,
                                  Long avatarAssetId) {

	public static PublicMemberProfile from(Member member) {
		return new PublicMemberProfile(member.getId(), member.getNickname(),
			member.getGender() == null ? null : member.getGender().name(),
			member.getAgeGroup() == null ? null : member.getAgeGroup().name(), member.getDescription(),
			member.getAvatarAssetId());
	}
}
