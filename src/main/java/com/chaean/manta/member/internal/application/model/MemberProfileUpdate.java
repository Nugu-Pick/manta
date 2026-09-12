package com.chaean.manta.member.internal.application.model;

import com.chaean.manta.member.entity.AgeGroup;
import com.chaean.manta.member.entity.Gender;

public record MemberProfileUpdate(String nickname, Gender gender, AgeGroup ageGroup, String description,
	Long avatarAssetId) {

	public static MemberProfileUpdate of(String nickname, Gender gender, AgeGroup ageGroup, String description,
		Long avatarAssetId) {
		return new MemberProfileUpdate(nickname, gender, ageGroup, description, avatarAssetId);
	}
}
