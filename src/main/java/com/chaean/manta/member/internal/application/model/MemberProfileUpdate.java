package com.chaean.manta.member.internal.application.model;

public record MemberProfileUpdate(String nickname, String gender, String ageGroup, String description,
                                  Long avatarAssetId) {
}
