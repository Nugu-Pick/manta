package com.chaean.manta.member.web.dto.response;

import com.chaean.manta.member.internal.application.PublicMemberProfile;

public record MemberPublicResponse(long id, String nickname, String gender, String ageGroup, String bio,
        Long avatarAssetId) {

    public static MemberPublicResponse from(PublicMemberProfile profile) {
        return new MemberPublicResponse(profile.id(), profile.nickname(), profile.gender(), profile.ageGroup(),
                profile.bio(), profile.avatarAssetId());
    }
}
