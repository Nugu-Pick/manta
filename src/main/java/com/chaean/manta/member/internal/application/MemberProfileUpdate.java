package com.chaean.manta.member.internal.application;

public record MemberProfileUpdate(String nickname, String gender, String ageGroup, String bio,
        Long avatarAssetId) {
}
