package com.chaean.manta.member.fixture;

import com.chaean.manta.member.entity.Member;

import org.springframework.test.util.ReflectionTestUtils;

public final class MemberFixture {

    private MemberFixture() {
    }

    public static Member create(long id, String subject, String email, String nickname) {
        Member member = Member.register(subject, email, nickname);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static Member createWithProfile(long id, String subject, String email, String nickname,
            String gender, String ageGroup, String description, Long avatarAssetId) {
        Member member = create(id, subject, email, nickname);
        member.updateProfile(nickname, gender, ageGroup, description, avatarAssetId);
        return member;
    }
}
