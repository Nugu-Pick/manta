package com.chaean.manta.member.internal.application;

import com.chaean.manta.member.entity.Member;

public record PublicMemberProfile(long id, String nickname, String gender, String ageGroup, String bio,
        Long avatarAssetId) {

    public static PublicMemberProfile from(Member member) {
        return new PublicMemberProfile(member.getId(), member.getNickname(), member.getGender(),
                member.getAgeGroup(), member.getBio(), member.getAvatarAssetId());
    }
}
