package com.chaean.manta.member.web.dto.response;

import com.chaean.manta.member.entity.MemberRole;
import com.chaean.manta.member.internal.application.model.MemberProfile;

public record MemberMeResponse(long id, String nickname, String email, String gender, String ageGroup,
                               String description,
                               Long avatarAssetId, MemberRole role) {

	public static MemberMeResponse from(MemberProfile profile) {
		return new MemberMeResponse(profile.id(), profile.nickname(), profile.email(), profile.gender(),
			profile.ageGroup(), profile.description(), profile.avatarAssetId(), profile.role());
	}
}
