package com.chaean.manta.member.internal.application;

import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberRole;

public record MemberProfile(long id, String nickname, String email, String gender, String ageGroup, String bio,
        Long avatarAssetId, MemberRole role) {

    public static MemberProfile from(Member member) {
        return new MemberProfile(member.getId(), member.getNickname(), member.getEmail(), member.getGender(),
                member.getAgeGroup(), member.getBio(), member.getAvatarAssetId(), member.getRole());
    }
}
