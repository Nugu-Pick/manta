package com.chaean.manta.member.web.dto.request;

import com.chaean.manta.member.entity.AgeGroup;
import com.chaean.manta.member.entity.Gender;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
	@Size(max = 32, message = "닉네임은 32자 이하여야 합니다.")
	String nickname,
	Gender gender,
	AgeGroup ageGroup,
	String description,
	@Positive(message = "avatarAssetId는 양수여야 합니다.")
	Long avatarAssetId) {
}
